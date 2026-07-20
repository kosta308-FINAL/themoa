package com.weaone.themoa.domain.recommend.service;

/**
 * 저축목표 실현가능성 판단 결과(이자 포함 실측 계산).
 * 원금만 나눈 게 아니라, 실제 판매중인 적금들의 진짜 금리로 "이자까지 합쳐서" 계산한다.
 *
 * @param hasGoal              목표를 입력했는지
 * @param reachableAtGoalMonths 사용자가 말한 목표기간 안에, 실제 상품 중 하나로 이자 포함 목표금액을 채울 수 있는지
 * @param actualMonthsNeeded   목표기간 안엔 못 채우지만 실제 판매중 상품으로 채우려면 몇 개월 필요한지
 *                              (reachableAtGoalMonths면 null, hopeless면 데이터 범위 안에서 못 찾았다는 뜻)
 * @param hopeless             우리가 가진 상품 데이터 범위(최장 36개월) 안에서도 도달 불가능한 수준인지
 */
public record GoalFeasibility(
        boolean hasGoal,
        boolean reachableAtGoalMonths,
        Integer actualMonthsNeeded,
        boolean hopeless
) {
    public static GoalFeasibility none() {
        return new GoalFeasibility(false, true, null, false);
    }

    public static GoalFeasibility reachable() {
        return new GoalFeasibility(true, true, null, false);
    }

    public static GoalFeasibility needsMoreTime(int months) {
        return new GoalFeasibility(true, false, months, false);
    }

    public static GoalFeasibility allHopeless() {
        return new GoalFeasibility(true, false, null, true);
    }
}
