package com.weaone.themoa.domain.recommend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.entity.SurplusFund;
import com.weaone.themoa.domain.recommend.dto.Recommendation;
import com.weaone.themoa.domain.recommend.dto.UserProfile;
import com.weaone.themoa.domain.recommend.dto.request.RecommendRequest;
import com.weaone.themoa.domain.recommend.dto.response.RecommendDefaultsResponse;
import com.weaone.themoa.domain.recommend.dto.response.RecommendResponse;
import com.weaone.themoa.domain.recommend.repository.RecommendBudgetRepository;
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
    private static final BigDecimal WON_PER_MANWON = BigDecimal.valueOf(10_000);

    private final RecommendationService recommendationService;
    private final RecommendBudgetRepository budgetRepository;
    private final RecommendSurplusFundRepository surplusFundRepository;

    public RecommendQueryService(RecommendationService recommendationService,
                                 RecommendBudgetRepository budgetRepository,
                                 RecommendSurplusFundRepository surplusFundRepository) {
        this.recommendationService = recommendationService;
        this.budgetRepository = budgetRepository;
        this.surplusFundRepository = surplusFundRepository;
    }

    /**
     * 추천 입력 폼의 기본값. 회원가입·소비내역 연동으로 이미 아는 값을 미리 채워준다.
     *
     * <p>월소득은 최근 급여주기 스냅샷(budget.salary_amount)에서 가져온다. 소득유형(고정월급/시급제)에 맞는
     * 계산이 이미 반영된 값이라 여기서 다시 계산하지 않는다. 소비가이드 설정 전이면 주기가 없어 null이다.
     *
     * <p>월 납입가능금액은 가장 최근 주기의 잉여금을 쓴다. 아직 적립된 잉여금이 없거나(가입 직후 등)
     * 잉여금이 최소 납입금액에 못 미치면(적자 주기는 음수로 적립된다) 기본값을 내려준다.
     */
    @Transactional(readOnly = true)
    public RecommendDefaultsResponse findDefaults(Long memberId) {
        Integer monthlyIncomeManwon = budgetRepository.findFirstByMember_IdOrderByCycleStartDateDesc(memberId)
                .map(Budget::getSalaryAmount)
                .map(salary -> salary.divide(WON_PER_MANWON, 0, RoundingMode.HALF_UP).intValue())
                .orElse(null);

        BigDecimal surplus = surplusFundRepository.findFirstByMember_IdOrderByYearMonthDesc(memberId)
                .map(SurplusFund::getAmount)
                .orElse(null);
        boolean usableSurplus = surplus != null
                && surplus.compareTo(BigDecimal.valueOf(MIN_MONTHLY_DEPOSIT_WON)) >= 0;
        int monthlyDepositWon = usableSurplus ? surplus.intValue() : DEFAULT_MONTHLY_DEPOSIT_WON;

        return new RecommendDefaultsResponse(monthlyIncomeManwon, monthlyDepositWon, usableSurplus);
    }

    @Transactional(readOnly = true)
    public RecommendResponse recommend(RecommendRequest request) {
        UserProfile profile = toProfile(request);

        List<Recommendation> recommendations = recommendationService.recommend(profile, TOP_N).stream()
                .map(RecommendQueryService::withCleanReasons)
                .toList();

        GoalFeasibility goal = recommendationService.assessGoal(profile);
        RecommendResponse.Feasibility feasibility = new RecommendResponse.Feasibility(
                goal.hasGoal(), goal.reachableAtGoalMonths(), goal.actualMonthsNeeded(), goal.hopeless());

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
