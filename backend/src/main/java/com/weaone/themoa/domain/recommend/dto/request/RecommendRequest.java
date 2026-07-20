package com.weaone.themoa.domain.recommend.dto.request;

import com.weaone.themoa.domain.recommend.service.PreferredPeriod;
import com.weaone.themoa.domain.recommend.service.RiskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 맞춤 금융상품 추천 요청. 기존 웹 프로토타입(GET /recommend)의 입력값을 JSON 요청으로 옮긴 것.
 * goalAmountWon이 없으면 goalMonths는 무시된다(서비스에서 강제).
 */
public record RecommendRequest(
        @Min(1) @Max(120) int age,
        Integer monthlyIncomeManwon,          // 월소득(만원). null이면 소득필터 건너뜀
        String employmentType,                // 직장인/프리랜서/무관. null·공백이면 무관
        boolean lowIncome,
        @NotNull RiskType riskType,           // STABLE / NEUTRAL / AGGRESSIVE
        @NotNull PreferredPeriod preferredPeriod, // SHORT / MID / LONG
        @Min(10000) int monthlyDepositWon,
        boolean acceptCondition,
        boolean needLiquidity,
        @Min(100000) Integer goalAmountWon,   // null 허용, 있으면 10만원 이상
        @Min(1) @Max(36) Integer goalMonths   // null 허용, 있으면 1~36개월
) {
}
