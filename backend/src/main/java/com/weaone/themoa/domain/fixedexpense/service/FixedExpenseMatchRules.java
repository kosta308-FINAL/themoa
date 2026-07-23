package com.weaone.themoa.domain.fixedexpense.service;

import java.math.BigDecimal;

/**
 * 수집 매칭 조건②·③의 순수 판정 로직(fixedExpense.md §5). 엔티티·저장소 의존이 없어 단위테스트로
 * 직접 검증한다 — 조건①(신원 대조)·④(주기 1회)는 저장소 조회가 필요해 {@link FixedExpenseMatchingService}에 남는다.
 */
public final class FixedExpenseMatchRules {

    private static final BigDecimal DOMESTIC_TOLERANCE = new BigDecimal("0.10");
    private static final BigDecimal FOREIGN_TYPE2_TOLERANCE = new BigDecimal("0.15");
    /** 관측치가 없을 때(직접 등록, 그룹 미보유) 쓰는 폴백 윈도우. */
    private static final int DEFAULT_PAY_DAY_WINDOW_DAYS = 3;
    /** 표본(3~4건)이 적어 실측 0도 그대로 믿지 않고 최소 이만큼은 남겨둔다. */
    private static final int MIN_PAY_DAY_WINDOW_DAYS = 1;
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

    /** 조건③: 결제일 ±windowDays. expectedPayDay가 없으면(edge) 항상 통과시킨다. */
    public static boolean isPayDayWithinWindow(Short expectedPayDay, int usedDayOfMonth, int windowDays) {
        if (expectedPayDay == null) {
            return true;
        }
        return Math.abs(usedDayOfMonth - expectedPayDay) <= windowDays;
    }

    /**
     * 결제일 윈도우를 고정값 대신 그 그룹에서 실제 관측된 흔들림(payDayVariance)에 맞춰 정한다
     * (fixedExpense.md §2) — 카드사마다 결제일 흔들림 폭이 다르다(고정일 결제는 흔들림 0, 첫 영업일
     * 기준 결제는 공휴일 배치에 따라 며칠씩 흔들림). 표본이 3~4건뿐이라 실측값에 여유 1일을 더하고,
     * 관측치가 없으면(직접 등록, group 없음) 기존 고정값으로 폴백한다.
     */
    public static int resolvePayDayWindow(Short observedPayDayVariance) {
        if (observedPayDayVariance == null) {
            return DEFAULT_PAY_DAY_WINDOW_DAYS;
        }
        return Math.max(observedPayDayVariance + 1, MIN_PAY_DAY_WINDOW_DAYS);
    }

    private static boolean withinTolerance(BigDecimal actual, BigDecimal expected, BigDecimal toleranceRatio) {
        BigDecimal diff = actual.subtract(expected).abs();
        BigDecimal allowed = expected.abs().multiply(toleranceRatio);
        return diff.compareTo(allowed) <= 0;
    }
}
