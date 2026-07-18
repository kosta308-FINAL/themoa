package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 월급 주기 라벨("yyyy-MM") 계산(fixedExpense.md §5). {@code member.payday}가 있으면 급여 주기
 * ({@link BudgetCyclePolicy}와 동일 계산식)를, 아직 소비 가이드를 설정하지 않아 payday가 없는 회원은
 * 달력 월로 폴백한다.
 */
public final class FixedExpenseCyclePolicy {

    public static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private FixedExpenseCyclePolicy() {
    }

    public static String currentYearMonth(Integer payday) {
        return yearMonthOf(LocalDate.now(ZONE_SEOUL), payday);
    }

    public static String yearMonthOf(LocalDate date, Integer payday) {
        if (payday == null) {
            return YearMonth.from(date).toString();
        }
        return BudgetCyclePolicy.cycleOf(payday, date).yearMonth();
    }
}
