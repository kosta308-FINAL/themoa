package com.weaone.themoa.domain.budget.dto.response;

import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.MemberWorkSchedule;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * S-01 오늘 현황·예산 기준(dayguide.md §3.1). 월급·급여일 미등록도 오류가 아니라
 * {@code setupRequired=true}로 반환한다. 금액은 계산 규칙(dailyBudget.md)에 따라 음수를 그대로 보존하고,
 * 하루 권장액·오늘 사용 가능 금액만 화면용 0 바닥이 이미 적용돼 있다.
 *
 * <p>{@code cycleSavingsAmount}는 {@code remainingAmount}(월 예산 − 실사용, 주기 마감 시 {@code surplus_fund}에
 * 그대로 적립되는 값)와는 다른 지표다 — 날짜별로 재구성한 그날의 하루 권장액과 그날 실사용액의 차이를
 * 경과일만큼 누적한 "절약 습관" 값이며, 하루 권장액이 매일 재계산되는 적응형 구조라 일반적으로
 * {@code remainingAmount}와 값이 갈린다(dailyBudget.md §1). 화면 표시용이라 0 바닥을 적용한다.
 */
public record SpendingGuideSummaryResponse(
        boolean setupRequired,
        List<String> missingFields,

        IncomeType incomeType,
        BigDecimal hourlyWage,
        List<WorkScheduleItemResponse> workSchedule,
        Integer payday,
        Integer pendingPayday,

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
        BigDecimal cycleSavingsAmount,

        boolean overCycleBudget,
        BigDecimal cycleOverspentAmount,
        boolean budgetUnaffordable) {

    /** 시급제(HOURLY) 요일별 근무시간 1건. 설정 변경 모달이 기존 값을 미리 채우는 데 쓴다. */
    public record WorkScheduleItemResponse(DayOfWeek dayOfWeek, BigDecimal hours) {
        public static WorkScheduleItemResponse from(MemberWorkSchedule schedule) {
            return new WorkScheduleItemResponse(schedule.getDayOfWeek(), schedule.getHours());
        }
    }

    /** 월급·급여일 중 하나라도 없어 소비 가이드 계산이 불가능한 상태(S-00A로 유도). */
    public static SpendingGuideSummaryResponse setupRequired(List<String> missingFields) {
        return new SpendingGuideSummaryResponse(true, missingFields,
                null, null, List.of(), null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null,
                false, null, false);
    }

    public static SpendingGuideSummaryResponse ready(
            IncomeType incomeType, BigDecimal hourlyWage, List<WorkScheduleItemResponse> workSchedule,
            Integer payday, Integer pendingPayday,
            String yearMonth, LocalDate cycleStartDate, LocalDate cycleEndDate, int remainingDays,
            BigDecimal salaryAmount, BigDecimal savingsGoalAmount, BigDecimal expectedFixedExpenseTotal,
            BigDecimal availableAmount, BigDecimal dailyRecommendedAmount, BigDecimal todayNetSpend,
            BigDecimal todayAvailableAmount, BigDecimal remainingAmount, BigDecimal cycleSavingsAmount,
            boolean overCycleBudget, BigDecimal cycleOverspentAmount, boolean budgetUnaffordable) {
        return new SpendingGuideSummaryResponse(false, List.of(),
                incomeType, hourlyWage, workSchedule, payday, pendingPayday,
                yearMonth, cycleStartDate, cycleEndDate, remainingDays,
                salaryAmount, savingsGoalAmount, expectedFixedExpenseTotal, availableAmount,
                dailyRecommendedAmount, todayNetSpend, todayAvailableAmount, remainingAmount, cycleSavingsAmount,
                overCycleBudget, cycleOverspentAmount, budgetUnaffordable);
    }
}
