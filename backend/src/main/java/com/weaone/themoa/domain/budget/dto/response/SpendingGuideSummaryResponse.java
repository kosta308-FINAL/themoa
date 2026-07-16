package com.weaone.themoa.domain.budget.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * S-01 오늘 현황·예산 기준(dayguide.md §3.1). 월급·급여일 미등록도 오류가 아니라
 * {@code setupRequired=true}로 반환한다. 금액은 계산 규칙(dailyBudget.md)에 따라 음수를 그대로 보존하고,
 * 하루 권장액·오늘 사용 가능 금액만 화면용 0 바닥이 이미 적용돼 있다.
 */
public record SpendingGuideSummaryResponse(
        boolean setupRequired,
        List<String> missingFields,

        String yearMonth,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        Integer remainingDays,

        BigDecimal salaryAmount,
        BigDecimal savingsGoalAmount,
        BigDecimal expectedFixedExpenseTotal,
        BigDecimal availableAmount,

        BigDecimal dailyRecommendedAmount,
        BigDecimal todayNetSpend,
        BigDecimal todayAvailableAmount,
        BigDecimal remainingAmount,

        boolean overCycleBudget,
        BigDecimal cycleOverspentAmount,
        boolean budgetUnaffordable) {

    /** 월급·급여일 중 하나라도 없어 소비 가이드 계산이 불가능한 상태(S-00A로 유도). */
    public static SpendingGuideSummaryResponse setupRequired(List<String> missingFields) {
        return new SpendingGuideSummaryResponse(true, missingFields,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                false, null, false);
    }

    public static SpendingGuideSummaryResponse ready(
            String yearMonth, LocalDate cycleStartDate, LocalDate cycleEndDate, int remainingDays,
            BigDecimal salaryAmount, BigDecimal savingsGoalAmount, BigDecimal expectedFixedExpenseTotal,
            BigDecimal availableAmount, BigDecimal dailyRecommendedAmount, BigDecimal todayNetSpend,
            BigDecimal todayAvailableAmount, BigDecimal remainingAmount, boolean overCycleBudget,
            BigDecimal cycleOverspentAmount, boolean budgetUnaffordable) {
        return new SpendingGuideSummaryResponse(false, List.of(),
                yearMonth, cycleStartDate, cycleEndDate, remainingDays,
                salaryAmount, savingsGoalAmount, expectedFixedExpenseTotal, availableAmount,
                dailyRecommendedAmount, todayNetSpend, todayAvailableAmount, remainingAmount,
                overCycleBudget, cycleOverspentAmount, budgetUnaffordable);
    }
}
