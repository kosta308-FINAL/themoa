package com.weaone.themoa.domain.fixedexpense.service;

import java.math.BigDecimal;

/**
 * 수집 매칭 조건②·③의 순수 판정 로직(fixedExpense.md §5). 엔티티·저장소 의존이 없어 단위테스트로
 * 직접 검증한다 — 조건①(신원 대조)·④(주기 1회)는 저장소 조회가 필요해 {@link FixedExpenseMatchingService}에 남는다.
 */
public final class FixedExpenseMatchRules {

    private static final BigDecimal DOMESTIC_TOLERANCE = new BigDecimal("0.10");
    private static final BigDecimal FOREIGN_TYPE2_TOLERANCE = new BigDecimal("0.15");
    private static final int PAY_DAY_WINDOW_DAYS = 3;
    private static final String CURRENCY_KRW = "KRW";

    private FixedExpenseMatchRules() {
    }

    /** 조건②: 국내 ±10% / 해외 type1(외화 원금 정확일치) / 해외 type2(원화 ±15% 폴백). */
    public static boolean isAmountMatch(String expectedCurrency, BigDecimal expectedAmount, BigDecimal expectedAmountKrw,
                                         String txCurrencyCode, BigDecimal txAmount, BigDecimal txOriginalAmount) {
        if (CURRENCY_KRW.equals(expectedCurrency)) {
            return withinTolerance(txAmount, expectedAmount, DOMESTIC_TOLERANCE);
        }
        if (txOriginalAmount != null && expectedCurrency.equals(txCurrencyCode)) {
            return txOriginalAmount.compareTo(expectedAmount) == 0;
        }
        return withinTolerance(txAmount, expectedAmountKrw, FOREIGN_TYPE2_TOLERANCE);
    }

    /** 조건③: 결제일 ±3일. expectedPayDay가 없으면(edge) 항상 통과시킨다. */
    public static boolean isPayDayWithinWindow(Short expectedPayDay, int usedDayOfMonth) {
        if (expectedPayDay == null) {
            return true;
        }
        return Math.abs(usedDayOfMonth - expectedPayDay) <= PAY_DAY_WINDOW_DAYS;
    }

    private static boolean withinTolerance(BigDecimal actual, BigDecimal expected, BigDecimal toleranceRatio) {
        BigDecimal diff = actual.subtract(expected).abs();
        BigDecimal allowed = expected.abs().multiply(toleranceRatio);
        return diff.compareTo(allowed) <= 0;
    }
}
