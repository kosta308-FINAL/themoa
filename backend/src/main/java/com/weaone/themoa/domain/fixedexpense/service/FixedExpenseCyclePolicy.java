package com.weaone.themoa.domain.fixedexpense.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 월급 주기 라벨("yyyy-MM") 계산. {@code Member.payday}가 아직 구현되지 않아(dailyBudget.md 스코프)
 * 이 도메인 전체가 달력 월로 폴백한다(fixedExpense.md §5) — payday가 도입되면 이 클래스만 교체하면 된다.
 */
public final class FixedExpenseCyclePolicy {

    public static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private FixedExpenseCyclePolicy() {
    }

    public static String currentYearMonth() {
        return YearMonth.now(ZONE_SEOUL).toString();
    }

    public static String yearMonthOf(LocalDate date) {
        return YearMonth.from(date).toString();
    }
}
