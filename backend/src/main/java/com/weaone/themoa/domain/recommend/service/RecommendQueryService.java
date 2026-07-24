package com.weaone.themoa.domain.recommend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.budget.entity.SurplusFund;
import com.weaone.themoa.domain.recommend.dto.Recommendation;
import com.weaone.themoa.domain.recommend.dto.UserProfile;
import com.weaone.themoa.domain.recommend.dto.request.RecommendRequest;
import com.weaone.themoa.domain.recommend.dto.response.RecommendDefaultsResponse;
import com.weaone.themoa.domain.recommend.dto.response.RecommendResponse;
import com.weaone.themoa.domain.recommend.entity.RecommendSnapshot;
import com.weaone.themoa.domain.recommend.repository.RecommendBudgetRepository;
import com.weaone.themoa.domain.recommend.repository.RecommendSnapshotRepository;
import com.weaone.themoa.domain.recommend.repository.RecommendSurplusFundRepository;

/**
 * 추천 요청(JSON)을 받아 기존 RecommendationService로 위임하고 응답 DTO로 조립한다.
 * 추천 알고리즘 자체는 RecommendationService에 있고, 여기서는 입력 매핑과 결과 조립만 담당한다.
 */
@Service
public class RecommendQueryService {

    private static final int TOP_N = 5;
    private static final String DEFAULT_EMPLOYMENT = "무관";

    /** 잉여금 기록이 아직 없을 때 쓰는 월 납입가능금액 기본값(원). */
    private static final int DEFAULT_MONTHLY_DEPOSIT_WON = 300_000;
    /** 추천 요청이 허용하는 최소 월 납입금액(RecommendRequest의 @Min과 맞춘다). */
    private static final int MIN_MONTHLY_DEPOSIT_WON = 10_000;
    /** 월 납입가능금액을 평균 낼 최근 주기 수. 한 주기의 우연한 흑자/적자에 좌우되지 않도록 여러 달을 본다. */
    private static final int SURPLUS_AVERAGE_CYCLE_COUNT = 3;
    private static final BigDecimal WON_PER_MANWON = BigDecimal.valueOf(10_000);

    private final RecommendationService recommendationService;
    private final RecommendBudgetRepository budgetRepository;
    private final RecommendSurplusFundRepository surplusFundRepository;
    private final RecommendSnapshotRepository snapshotRepository;
    private final MemberRepository memberRepository;

    public RecommendQueryService(RecommendationService recommendationService,
                                 RecommendBudgetRepository budgetRepository,
                                 RecommendSurplusFundRepository surplusFundRepository,
                                 RecommendSnapshotRepository snapshotRepository,
                                 MemberRepository memberRepository) {
        this.recommendationService = recommendationService;
        this.budgetRepository = budgetRepository;
        this.surplusFundRepository = surplusFundRepository;
        this.snapshotRepository = snapshotRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 추천 입력 폼의 기본값. 회원가입·소비내역 연동으로 이미 아는 값을 미리 채워준다.
     *
     * <p>월소득은 최근 급여주기 스냅샷(budget.salary_amount)에서 가져온다. 소득유형(고정월급/시급제)에 맞는
     * 계산이 이미 반영된 값이라 여기서 다시 계산하지 않는다. 소비가이드 설정 전이면 주기가 없어 null이다.
     *
     * <p>월 납입가능금액은 최근 {@value #SURPLUS_AVERAGE_CYCLE_COUNT}개 주기 잉여금의 평균을 쓴다. 적자 주기는
     * 음수로 적립되어 있는데(SurplusFund 참고), 평균 낼 때도 0으로 깎지 않고 그대로 더한다 — 한두 달 우연히
     * 많이 남았다고 평균이 부풀려지거나, 반대로 적자가 있었는데도 무시되면 안 되기 때문이다. 아직 적립된
     * 잉여금이 없거나(가입 직후 등) 평균이 최소 납입금액에 못 미치면 기본값을 내려준다.
     */
    @Transactional(readOnly = true)
    public RecommendDefaultsResponse findDefaults(Long memberId) {
        Integer monthlyIncomeManwon = budgetRepository.findFirstByMember_IdOrderByCycleStartDateDesc(memberId)
                .map(Budget::getSalaryAmount)
                .map(salary -> salary.divide(WON_PER_MANWON, 0, RoundingMode.HALF_UP).intValue())
                .orElse(null);

        List<SurplusFund> recentCycles = surplusFundRepository.findByMember_IdOrderByYearMonthDesc(
                memberId, PageRequest.of(0, SURPLUS_AVERAGE_CYCLE_COUNT));
        BigDecimal averageSurplus = recentCycles.isEmpty()
                ? null
                : recentCycles.stream()
                        .map(SurplusFund::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(recentCycles.size()), 0, RoundingMode.HALF_UP);
        boolean usableSurplus = averageSurplus != null
                && averageSurplus.compareTo(BigDecimal.valueOf(MIN_MONTHLY_DEPOSIT_WON)) >= 0;
        int monthlyDepositWon = usableSurplus ? averageSurplus.intValue() : DEFAULT_MONTHLY_DEPOSIT_WON;

        return new RecommendDefaultsResponse(monthlyIncomeManwon, monthlyDepositWon, usableSurplus);
    }

    /**
     * 추천 실행. 결과 top N을 회원의 "최근 추천 기록"으로 저장한다 — 나중에 그 상품의 금리·우대조건이
     * 바뀌면 알려주기 위해서다(북마크와 함께 변경 알림 대상이 된다).
     */
    @Transactional
    public RecommendResponse recommend(Long memberId, RecommendRequest request) {
        UserProfile profile = toProfile(request);

        List<Recommendation> recommendations = recommendationService.recommend(profile, TOP_N).stream()
                .map(RecommendQueryService::withCleanReasons)
                .toList();

        GoalFeasibility goal = recommendationService.assessGoal(profile);
        RecommendResponse.Feasibility feasibility = new RecommendResponse.Feasibility(
                goal.hasGoal(), goal.reachableAtGoalMonths(), goal.actualMonthsNeeded(), goal.hopeless());

        saveSnapshot(memberId, recommendations);
        return new RecommendResponse(feasibility, recommendations);
    }

    /**
     * 목표금액이 없으면 목표기간은 무시한다(웹 프로토타입과 동일 규칙) — 목표금액 없이 기간만 들어오면
     * 선호기간을 조용히 덮어써 랭킹이 바뀌는 문제를 막기 위함.
     */
    private UserProfile toProfile(RecommendRequest request) {
        Integer goalMonths = request.goalAmountWon() == null ? null : request.goalMonths();
        String employment = (request.employmentType() == null || request.employmentType().isBlank())
                ? DEFAULT_EMPLOYMENT
                : request.employmentType();

        return new UserProfile(
                request.age(),
                request.monthlyIncomeManwon(),
                employment,
                request.lowIncome(),
                request.riskType(),
                request.preferredPeriod(),
                request.monthlyDepositWon(),
                request.acceptCondition(),
                request.needLiquidity(),
                request.goalAmountWon(),
                goalMonths,
                null);
    }

    /** 최근 추천 기록을 새 결과로 교체한다(최신 1회분만 유지). */
    private void saveSnapshot(Long memberId, List<Recommendation> recommendations) {
        snapshotRepository.deleteByMember_Id(memberId);
        if (recommendations.isEmpty()) {
            return;
        }
        Member member = memberRepository.getReferenceById(memberId);
        LocalDateTime now = LocalDateTime.now();
        List<RecommendSnapshot> rows = new java.util.ArrayList<>(recommendations.size());
        for (int i = 0; i < recommendations.size(); i++) {
            Recommendation item = recommendations.get(i);
            if (item.id() == null) {
                continue;
            }
            // 추천은 예·적금만 다루므로 대상 유형은 SAVINGS_PRODUCT로 고정된다.
            rows.add(RecommendSnapshot.of(member, "SAVINGS_PRODUCT", item.id(), i + 1, now));
        }
        snapshotRepository.saveAll(rows);
    }

    /** 추천 이유에 붙는 내부 채점 표기 "(+N)"는 사용자용이 아니라서 응답에서 떼어낸다. */
    private static Recommendation withCleanReasons(Recommendation r) {
        List<String> cleaned = r.reasons().stream()
                .map(reason -> reason.replaceAll("\\s*\\(\\+\\d+\\)\\s*$", ""))
                .toList();
        return new Recommendation(
                r.id(), r.company(), r.productName(), r.type(), r.score(), r.bestRate(), r.bestRateTerm(),
                cleaned, r.llmReason(), r.maturityAmountWon(), r.goalMonthlyWon(), r.goalMaturityAmountWon());
    }
}
