package com.weaone.themoa.domain.recommend.service;

import com.weaone.themoa.domain.recommend.dto.Recommendation;
import com.weaone.themoa.domain.recommend.dto.UserProfile;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import com.weaone.themoa.domain.recommend.repository.SavingsProductRepository;
import com.weaone.themoa.domain.recommend.entity.SavingsType;

/**
 * 맞춤 추천 서비스 (예금/적금).
 * 흐름: 판매중 조회 → 1단계 하드필터 → 2단계 소프트필터(점수) → 점수 순 상위 topN이 최종 순위.
 * 3단계는 그 순위를 바꾸지 않고 LLM이 자연어 이유만 덧붙인다(점수 1등 = 항상 1위).
 */
@Service
public class RecommendationService {

    private final SavingsProductRepository repository;
    private final LlmSelector llmSelector;
    private final com.weaone.themoa.domain.financialsearch.service.BankNameFormatter bankNameFormatter;

    public RecommendationService(SavingsProductRepository repository, LlmSelector llmSelector,
                                 com.weaone.themoa.domain.financialsearch.service.BankNameFormatter bankNameFormatter) {
        this.repository = repository;
        this.llmSelector = llmSelector;
        this.bankNameFormatter = bankNameFormatter;
    }

    @Transactional(readOnly = true)
    public List<Recommendation> recommend(UserProfile profile, int topN) {
        List<SavingsProduct> selling = repository.findAllSellingWithOptions();

        // 금리 순위는 "목표 개월수 기준" 금리로, 전체 판매중 상품을 놓고 계산한다(정확한 개월 비교).
        RateRanking ranking = new RateRanking(selling, profile.effectiveTargetMonths());

        // 1~2단계: 하드필터 통과 상품에 점수 → 점수순 정렬 → 상위 topN = 최종 순위(확정)
        // 추천은 "월 납입가능금액" 기준이라 매달 납입하는 적금(SAVING)만 대상으로 한다. 정기예금(DEPOSIT)은
        // 목돈을 한 번에 예치하는 상품이라 월납입 기준 만기금액·비교가 성립하지 않아 추천 목록에서 제외한다.
        List<Recommendation> ranked = selling.stream()
                .filter(p -> p.getProductType() == SavingsType.SAVING)
                .filter(p -> HardFilter.passes(p, profile))
                .map(p -> toRecommendation(p, profile, ranking))
                .sorted(Comparator.comparingInt(Recommendation::score).reversed()
                        .thenComparing(Recommendation::bestRate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Recommendation::company))
                .limit(topN)
                .toList();

        // 3단계: 순위는 그대로 두고, LLM이 각 상품에 자연어 이유만 붙인다.
        List<Recommendation> withReasons = llmSelector.explain(profile, ranked);
        return withReasons != null ? withReasons : ranked;
    }

    /**
     * 저축목표 실현가능성을 "실제 판매중 적금의 진짜 금리로 이자까지 포함해서" 판단한다.
     * 원금만 나눈 계산이 아니다 — 이자 덕분에 목표기간 안에 채워지면 그대로 통과시키고,
     * 안 되면 실제 상품 데이터 범위 안에서 진짜 몇 개월 필요한지 찾는다.
     */
    @Transactional(readOnly = true)
    public GoalFeasibility assessGoal(UserProfile profile) {
        if (profile.goalAmountWon() == null || profile.goalMonths() == null) {
            return GoalFeasibility.none();
        }
        List<SavingsProduct> eligible = repository.findAllSellingWithOptions().stream()
                .filter(p -> p.getProductType() == SavingsType.SAVING)   // 월납입 개념이 맞는 적금만
                .filter(p -> HardFilter.passes(p, profile))
                .toList();

        long goalAmount = profile.goalAmountWon();
        long monthlyWon = profile.monthlyDepositWon();

        // ① 목표기간 "이내"(그 기간과 같거나 더 짧은 실존 기간) 중 하나로라도 이자 포함해서 채워지면 통과.
        // 목표기간에 정확히 일치하는 상품이 없어도, 여유가 있으면 더 짧은 기간 상품으로 얼마든지 먼저 채울 수 있다
        // (예: 월30만원으로 목표100만원/60개월이면 60개월짜리 상품이 없어도 4개월이면 이미 채워짐 — 이건 "불가능"이 아니라 "여유있음").
        if (bestMaturityWithinTerm(eligible, monthlyWon, profile.goalMonths()) >= goalAmount) {
            return GoalFeasibility.reachable();
        }

        // ② 목표기간보다 긴, 실제 데이터에 존재하는 기간들 중에서 채워지는 최단 기간을 찾는다
        for (int term : availableTermsLongerThan(eligible, profile.goalMonths())) {
            if (bestMaturityAt(eligible, monthlyWon, term) >= goalAmount) {
                return GoalFeasibility.needsMoreTime(term);
            }
        }
        return GoalFeasibility.allHopeless();
    }

    /** 목표기간과 같거나 더 짧은 실존 가입기간들 중, 만기수령액이 가장 큰 값(여유 있으면 어차피 짧은 기간에서 이미 채워짐). */
    private long bestMaturityWithinTerm(List<SavingsProduct> eligible, long monthlyWon, int maxMonths) {
        long best = 0;
        for (SavingsProduct p : eligible) {
            for (SavingsProductOption o : p.getOptions()) {
                if (o.getTermMonth() == null || o.getTermMonth() > maxMonths) {
                    continue;
                }
                long maturity = maturityOf(o, monthlyWon);
                if (maturity > best) {
                    best = maturity;
                }
            }
        }
        return best;
    }

    /** 특정 개월수(term)에 정확히 일치하는 옵션들 중, 월납입액을 넣었을 때 가장 큰 만기수령액. 없으면 0. */
    private long bestMaturityAt(List<SavingsProduct> eligible, long monthlyWon, int termMonths) {
        long best = 0;
        for (SavingsProduct p : eligible) {
            for (SavingsProductOption o : p.getOptions()) {
                if (o.getTermMonth() == null || o.getTermMonth() != termMonths) {
                    continue;
                }
                long maturity = maturityOf(o, monthlyWon);
                if (maturity > best) {
                    best = maturity;
                }
            }
        }
        return best;
    }

    /** 목표기간보다 긴, 실제 데이터에 존재하는 가입기간(개월) 목록을 오름차순으로. */
    private Set<Integer> availableTermsLongerThan(List<SavingsProduct> eligible, int months) {
        Set<Integer> terms = new TreeSet<>();
        for (SavingsProduct p : eligible) {
            for (SavingsProductOption o : p.getOptions()) {
                if (o.getTermMonth() != null && o.getTermMonth() > months) {
                    terms.add(o.getTermMonth());
                }
            }
        }
        return terms;
    }

    private long maturityOf(SavingsProductOption o, long monthlyWon) {
        BigDecimal rate = o.getMaxRate() != null ? o.getMaxRate() : o.getBaseRate();
        boolean compound = "M".equals(o.getRateTypeCode());
        return MaturityCalculator.installmentMaturity(monthlyWon, rate, o.getTermMonth(), compound);
    }

    private Recommendation toRecommendation(SavingsProduct p, UserProfile profile, RateRanking ranking) {
        SoftScorer.ScoreResult result = SoftScorer.score(p, profile, ranking);
        // 표시 금리·기간도 목표 개월수에 가장 가까운 옵션에서 가져온다.
        var option = RateRanking.targetOption(p, profile.effectiveTargetMonths());
        BigDecimal rate = option == null ? null
                : (option.getMaxRate() != null ? option.getMaxRate() : option.getBaseRate());

        // 적금만 "월납입액(여력 전액)을 여기 넣으면 총 얼마 모이는지" 계산(예금은 월납입 개념이 안 맞음).
        Long maturity = (option != null && p.getProductType() == SavingsType.SAVING)
                ? maturityOf(option, profile.monthlyDepositWon())
                : null;

        // 목표가 있고 여력이 목표보다 큰 경우 "목표만 채우려면 매월 얼마씩이면 되는지"도 함께 계산해서 보여준다.
        Long goalMonthly = null;
        Long goalMaturity = null;
        if (option != null && p.getProductType() == SavingsType.SAVING && profile.goalAmountWon() != null) {
            BigDecimal optRate = option.getMaxRate() != null ? option.getMaxRate() : option.getBaseRate();
            boolean compound = "M".equals(option.getRateTypeCode());
            long required = MaturityCalculator.requiredMonthlyForGoal(
                    profile.goalAmountWon(), optRate, option.getTermMonth(), compound);
            // 목표달성에 필요한 금액이 여력보다 작을 때만 의미가 있다(여력 초과면 그냥 "부족" 상황이라 표시 안 함).
            if (required < profile.monthlyDepositWon()) {
                goalMonthly = required;
                goalMaturity = MaturityCalculator.installmentMaturity(required, optRate, option.getTermMonth(), compound);
            }
        }

        return new Recommendation(
                p.getId(),
                bankNameFormatter.toDisplayName(p.getCompanyName()),
                p.getProductName(),
                p.getProductType().name(),
                result.score(),
                rate,
                option == null ? null : option.getTermMonth(),
                result.reasons(),
                null,
                maturity,
                goalMonthly,
                goalMaturity);
    }
}
