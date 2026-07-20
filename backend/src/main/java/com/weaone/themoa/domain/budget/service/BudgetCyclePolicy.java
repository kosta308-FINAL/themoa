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

    /**
     * 급여일 변경 직후 첫 주기(브리지 주기, payday.md §급여일 변경). 이전 주기 종료 다음날부터, 새 payday가
     * 그 뒤 처음 도래하는 날 전날까지로 계산한다 — {@code cycleOf(newPayday, today)}로 그냥 재계산하면 새
     * payday가 이전 주기 종료일보다 앞선 날짜를 시작일로 잡아 두 주기가 겹치거나(당김) 사이에 공백이
     * 생길(미룸) 수 있어 별도 계산이 필요하다. 이 주기 종료일 다음날은 새 payday의 실제 도래일과 정확히
     * 일치하므로, 그 다음부터는 다시 {@link #cycleOf}만으로 이어진다.
     */
    public static BudgetCycle bridgeCycle(LocalDate previousCycleEndDate, int newPayday) {
        LocalDate start = previousCycleEndDate.plusDays(1);
        LocalDate nextOccurrence = firstOccurrenceStrictlyAfter(start, newPayday);
        LocalDate end = nextOccurrence.minusDays(1);
        return new BudgetCycle(YearMonth.from(start).toString(), start, end);
    }

    private static LocalDate firstOccurrenceStrictlyAfter(LocalDate date, int payday) {
        LocalDate candidate = effectivePayday(YearMonth.from(date), payday);
        return candidate.isAfter(date) ? candidate : effectivePayday(YearMonth.from(date).plusMonths(1), payday);
    }

    /** 남은 일수 = 오늘부터 주기 종료일까지, 오늘 포함. 최소 1(종료일 당일). */
    public static int remainingDays(LocalDate today, LocalDate cycleEndDate) {
        return (int) ChronoUnit.DAYS.between(today, cycleEndDate) + 1;
    }

    public record BudgetCycle(String yearMonth, LocalDate cycleStartDate, LocalDate cycleEndDate) {
    }
}
