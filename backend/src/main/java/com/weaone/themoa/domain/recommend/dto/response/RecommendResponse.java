package com.weaone.themoa.domain.recommend.dto.response;

import java.util.List;

import com.weaone.themoa.domain.recommend.dto.Recommendation;

/**
 * 맞춤 추천 결과. recommendations는 "월 납입가능액(여력) 전액 기준" 상위 추천이다.
 * feasibility(저축목표 실현가능성)는 목표를 입력했을 때만 의미가 있다.
 */
public record RecommendResponse(
        Feasibility feasibility,
        List<Recommendation> recommendations
) {
    /**
     * 저축목표 실현가능성 요약(이자 포함 실측). 프론트는 이 플래그로 경고 문구를 분기한다.
     *
     * @param hasGoal               목표 입력 여부
     * @param reachableAtGoalMonths 목표기간 안에 목표금액 달성 가능한지
     * @param actualMonthsNeeded    목표기간 안엔 안 되지만 실제로 몇 개월이면 되는지(가능하면 null)
     * @param hopeless              상품 데이터 범위(최장 36개월)로도 도달 불가한지
     */
    public record Feasibility(
            boolean hasGoal,
            boolean reachableAtGoalMonths,
            Integer actualMonthsNeeded,
            boolean hopeless
    ) {
    }
}
