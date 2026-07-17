package com.weaone.themoa.domain.budget.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 급여일 기준 예산 주기 계산(dailyBudget.md §1, MOA-S-BUD-BGT-02). 한 주기 = 월급일 ~ 다음 월급 전날이며
 * 달력 월과 혼용하지 않는다. 명목 급여일이 그 달에 없으면(29~31일) 말일로 당긴다. 주말·공휴일의 실제 입금일
 * 변동은 반영하지 않는다 — 명목 급여일만으로 계산한다.
 */
public final class BudgetCyclePolicy {

    public static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private BudgetCyclePolicy() {
    }

    /** {@code today}가 속한 급여 주기. */
    public static BudgetCycle cycleOf(int payday, LocalDate today) {
        LocalDate startCandidate = effectivePayday(YearMonth.from(today), payday);
        LocalDate cycleStart = today.isBefore(startCandidate)
                ? effectivePayday(YearMonth.from(today).minusMonths(1), payday)
                : startCandidate;
        LocalDate nextStart = effectivePayday(YearMonth.from(cycleStart).plusMonths(1), payday);
        LocalDate cycleEnd = nextStart.minusDays(1);
        return new BudgetCycle(YearMonth.from(cycleStart).toString(), cycleStart, cycleEnd);
    }

    /** 명목 급여일을 그 달 실제 날짜로 당긴다(31일·2월 등 없는 날은 말일). */
    private static LocalDate effectivePayday(YearMonth yearMonth, int payday) {
        return yearMonth.atDay(Math.min(payday, yearMonth.lengthOfMonth()));
    }

    /** 남은 일수 = 오늘부터 주기 종료일까지, 오늘 포함. 최소 1(종료일 당일). */
    public static int remainingDays(LocalDate today, LocalDate cycleEndDate) {
        return (int) ChronoUnit.DAYS.between(today, cycleEndDate) + 1;
    }

    public record BudgetCycle(String yearMonth, LocalDate cycleStartDate, LocalDate cycleEndDate) {
    }
}
