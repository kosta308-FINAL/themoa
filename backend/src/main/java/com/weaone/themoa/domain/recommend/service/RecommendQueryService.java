package com.weaone.themoa.domain.recommend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

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
import com.weaone.themoa.domain.recommend.entity.FinancialProfile;
import com.weaone.themoa.domain.recommend.entity.RecommendSnapshot;
import com.weaone.themoa.domain.recommend.repository.FinancialProfileRepository;
import com.weaone.themoa.domain.recommend.repository.RecommendBudgetRepository;
import com.weaone.themoa.domain.recommend.repository.RecommendSnapshotRepository;
import com.weaone.themoa.domain.recommend.repository.RecommendSurplusFundRepository;
import com.weaone.themoa.domain.recommend.repository.RecommendSurplusFundTransferRepository;

/**
 * 추천 요청(JSON)을 받아 기존 RecommendationService로 위임하고 응답 DTO로 조립한다.
 * 추천 알고리즘 자체는 RecommendationService에 있고, 여기서는 입력 매핑과 결과 조립만 담당한다.
 */
@Service
public class RecommendQueryService {

    private static final int TOP_N = 5;
    private static final String DEFAULT_EMPLOYMENT = "무관";
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    /** 잉여금 기록이 아직 없을 때 쓰는 월 납입가능금액 기본값(원). */
    private static final int DEFAULT_MONTHLY_DEPOSIT_WON = 300_000;
    /** 추천 요청이 허용하는 최소 월 납입금액(RecommendRequest의 @Min과 맞춘다). */
    private static final int MIN_MONTHLY_DEPOSIT_WON = 10_000;
    private static final BigDecimal WON_PER_MANWON = BigDecimal.valueOf(10_000);

    private final RecommendationService recommendationService;
    private final RecommendBudgetRepository budgetRepository;
    private final RecommendSurplusFundRepository surplusFundRepository;
    private final RecommendSurplusFundTransferRepository surplusFundTransferRepository;
    private final RecommendSnapshotRepository snapshotRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final MemberRepository memberRepository;

    public RecommendQueryService(RecommendationService recommendationService,
                                 RecommendBudgetRepository budgetRepository,
                                 RecommendSurplusFundRepository surplusFundRepository,
                                 RecommendSurplusFundTransferRepository surplusFundTransferRepository,
                                 RecommendSnapshotRepository snapshotRepository,
                                 FinancialProfileRepository financialProfileRepository,
                                 MemberRepository memberRepository) {
        this.recommendationService = recommendationService;
        this.budgetRepository = budgetRepository;
        this.surplusFundRepository = surplusFundRepository;
        this.surplusFundTransferRepository = surplusFundTransferRepository;
        this.snapshotRepository = snapshotRepository;
        this.financialProfileRepository = financialProfileRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 추천 입력 폼의 기본값. 계산 가능한 회원 값과 마지막으로 저장한 추천 조건을 미리 채워준다.
     *
     * <p>월소득은 최근 급여주기 스냅샷(budget.salary_amount)에서 가져온다. 소득유형(고정월급/시급제)에 맞는
     * 계산이 이미 반영된 값이라 여기서 다시 계산하지 않는다. 소비가이드 설정 전이면 주기가 없어 null이다.
     *
     * <p>월 납입가능금액은 전체 급여주기 잉여금에서 이미 이번 주기들로 가져간 금액을 뺀 순잔액의
     * 주기당 평균을 쓴다. 원본 잉여금 행은 당시 결과로 보존하면서, 사용자가 다른 용도로 배정한 돈은
     * 추천 여력에서 즉시 제외한다. 아직 적립된 잉여금이 없을 때만 기본값을 내려준다.
     */
    @Transactional(readOnly = true)
    public RecommendDefaultsResponse findDefaults(Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);
        int age = Period.between(member.getBirthDate(), LocalDate.now(SEOUL_ZONE)).getYears();

        Integer monthlyIncomeManwon = budgetRepository.findFirstByMember_IdOrderByCycleStartDateDesc(memberId)
                .map(Budget::getSalaryAmount)
                .map(salary -> salary.divide(WON_PER_MANWON, 0, RoundingMode.HALF_UP).intValue())
                .orElse(null);

        List<SurplusFund> allCycles = surplusFundRepository.findByMember_Id(memberId);
        BigDecimal transferredSurplus = surplusFundTransferRepository.sumAmountByMember_Id(memberId);
        BigDecimal averageSurplus = allCycles.isEmpty()
                ? null
                : allCycles.stream()
                        .map(SurplusFund::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .subtract(transferredSurplus)
                        .divide(BigDecimal.valueOf(allCycles.size()), 0, RoundingMode.HALF_UP);
        boolean usableSurplus = averageSurplus != null;
        int monthlyDepositWon = usableSurplus
                ? averageSurplus.max(BigDecimal.valueOf(MIN_MONTHLY_DEPOSIT_WON)).intValue()
                : DEFAULT_MONTHLY_DEPOSIT_WON;

        FinancialProfile profile = financialProfileRepository.findByMember_Id(memberId).orElse(null);
        return new RecommendDefaultsResponse(
                age,
                monthlyIncomeManwon,
                monthlyDepositWon,
                usableSurplus,
                profile == null ? null : profile.getEmploymentType(),
                profile != null && profile.isLowIncome(),
                profile == null ? null : profile.getRiskType(),
                profile == null ? null : profile.getPreferredPeriod(),
                profile != null && profile.isAcceptCondition(),
                profile != null && profile.isNeedLiquidity(),
                profile == null ? null : profile.getGoalAmountWon(),
                profile == null ? null : profile.getGoalMonths());
    }

    /**
     * 추천 실행. 결과 top N을 회원의 "최근 추천 기록"으로 저장한다 — 나중에 그 상품의 금리·우대조건이
     * 바뀌면 알려주기 위해서다(북마크와 함께 변경 알림 대상이 된다).
     */
    @Transactional
    public RecommendResponse recommend(Long memberId, RecommendRequest request) {
        UserProfile profile = toProfile(request);
        saveFinancialProfile(memberId, profile);

        List<Recommendation> recommendations = recommendationService.recommend(profile, TOP_N).stream()
                .map(RecommendQueryService::withCleanReasons)
                .toList();

        GoalFeasibility goal = recommendationService.assessGoal(profile);
        RecommendResponse.Feasibility feasibility = new RecommendResponse.Feasibility(
                goal.hasGoal(), goal.reachableAtGoalMonths(), goal.actualMonthsNeeded(), goal.hopeless());

        saveSnapshot(memberId, recommendations);
        return new RecommendResponse(feasibility, recommendations);
    }

    private void saveFinancialProfile(Long memberId, UserProfile profile) {
        FinancialProfile financialProfile = financialProfileRepository.findByMember_Id(memberId)
                .orElseGet(() -> FinancialProfile.create(memberRepository.getReferenceById(memberId)));
        financialProfile.update(
                profile.employmentType(),
                profile.lowIncome(),
                profile.riskType(),
                profile.preferredPeriod(),
                profile.acceptCondition(),
                profile.needLiquidity(),
                profile.goalAmountWon(),
                profile.goalMonths());
        financialProfileRepository.save(financialProfile);
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
