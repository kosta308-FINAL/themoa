package com.weaone.themoa.domain.recommend.service;

import java.math.BigDecimal;

/**
 * 정액적립식 적금의 만기 수령액(세전 추정치)을 계산한다.
 * 매월 같은 금액을 넣고, 각 회차 납입금이 만기까지 남은 기간만큼 이자가 붙는다고 가정한다.
 * (세금 미반영 근사치 — 실제 수령액은 이자소득세 등으로 다를 수 있음)
 */
public final class MaturityCalculator {

    private MaturityCalculator() {
    }

    /**
     * 정액적립식 적금 만기 수령액 = 원금 + 이자.
     *
     * @param monthlyWon        매월 납입액(원)
     * @param annualRatePercent 연 이율(%, 예: 7.00)
     * @param months            납입 개월수
     * @param compound          true=복리(월복리 근사), false=단리
     */
    public static long installmentMaturity(long monthlyWon, BigDecimal annualRatePercent, int months, boolean compound) {
        if (months <= 0) {
            return 0;
        }
        long principal = monthlyWon * months;
        if (annualRatePercent == null) {
            return principal;
        }
        double rate = annualRatePercent.doubleValue() / 100.0;

        if (!compound) {
            // 단리 정액적립식 공식: 이자 = 월납입액 × 연이율 × n(n+1) / 24
            // (1회차는 만기까지 n개월, 2회차는 n-1개월, ... 이자가 붙는다고 가정한 합)
            double interest = monthlyWon * rate * months * (months + 1) / 24.0;
            return principal + Math.round(interest);
        }

        // 복리(월복리 근사): 각 회차 납입금이 남은 개월수만큼 월복리로 불어난다고 가정
        double monthlyRate = rate / 12.0;
        double total = 0;
        for (int k = 1; k <= months; k++) {
            int remaining = months - k + 1;
            total += monthlyWon * Math.pow(1 + monthlyRate, remaining);
        }
        return Math.round(total);
    }

    /**
     * 목표금액을 이 개월수 안에 채우려면 매월 얼마씩 넣어야 하는지 역산(원단위 올림).
     * 만기수령액은 월납입액에 정확히 비례하므로, "1원 기준 만기액"을 구해 목표금액을 그걸로 나눈다.
     */
    public static long requiredMonthlyForGoal(long goalAmountWon, BigDecimal annualRatePercent, int months, boolean compound) {
        long maturityPerWon = installmentMaturity(1_000_000L, annualRatePercent, months, compound);
        if (maturityPerWon <= 0) {
            return goalAmountWon / Math.max(months, 1);
        }
        return (long) Math.ceil(goalAmountWon * 1_000_000.0 / maturityPerWon);
    }
}
