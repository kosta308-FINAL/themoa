package com.weaone.themoa.domain.recommend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weaone.themoa.domain.recommend.dto.Recommendation;
import com.weaone.themoa.domain.recommend.dto.UserProfile;
import com.weaone.themoa.domain.recommend.dto.request.RecommendRequest;
import com.weaone.themoa.domain.recommend.dto.response.RecommendResponse;

/**
 * 추천 요청(JSON)을 받아 기존 RecommendationService로 위임하고 응답 DTO로 조립한다.
 * 추천 알고리즘 자체는 RecommendationService에 있고, 여기서는 입력 매핑과 결과 조립만 담당한다.
 */
@Service
public class RecommendQueryService {

    private static final int TOP_N = 5;
    private static final String DEFAULT_EMPLOYMENT = "무관";

    private final RecommendationService recommendationService;

    public RecommendQueryService(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
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
