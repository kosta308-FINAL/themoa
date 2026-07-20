package com.weaone.themoa.domain.budget.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 예산 파생값 계산 규칙(dailyBudget.md, erd.md §6) — 음수 보존과 하루 권장액 0 바닥. */
class BudgetTest {

    private Budget budget(String salary, String fixedTotal, String savingsGoal) {
        return Budget.openCycle(null, "2026-07", LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 4),
                new BigDecimal(salary), new BigDecimal(savingsGoal), new BigDecimal(fixedTotal), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("월 예산 = 월급 − 고정지출 − 저축목표. 고정지출은 원화 스냅샷 합이라 500,022가 아니라 530,000이 빠진다")
    void availableUsesKrwSnapshot() {
        // 월세 500,000 + Claude $22 환산 30,000 = 530,000 (달러 원금 22를 그대로 더한 500,022가 아님)
        Budget budget = budget("2000000", "530000", "0");

        assertThat(budget.getAvailableAmount(BigDecimal.ZERO)).isEqualByComparingTo("1470000");
    }

    @Test
    @DisplayName("월 예산은 음수를 그대로 흘린다 — 월급200 고정150 저축60 = −10만원")
    void availableCanBeNegative() {
        Budget budget = budget("2000000", "1500000", "600000");

        assertThat(budget.getAvailableAmount(BigDecimal.ZERO)).isEqualByComparingTo("-100000");
    }

    @Test
    @DisplayName("남은 예산도 음수 그대로 — 초과지출분을 잃지 않는다")
    void remainingCanBeNegative() {
        Budget budget = budget("2000000", "0", "0");

        assertThat(budget.getRemainingAmount(new BigDecimal("2200000"), BigDecimal.ZERO))
                .isEqualByComparingTo("-200000");
    }

    @Test
    @DisplayName("하루 권장액 = (월 예산 − 어제까지 순지출) ÷ 남은일수, 내림")
    void dailyRecommendedFloorsDown() {
        Budget budget = budget("1000000", "0", "0");

        // (1,000,000 − 250,000) / 20 = 37,500
        assertThat(budget.getDailyRecommendedAmount(new BigDecimal("250000"), 20, BigDecimal.ZERO))
                .isEqualByComparingTo("37500");
    }

    @Test
    @DisplayName("하루 권장액만 0에서 바닥을 건다 — 음수여도 −4,000이 아니라 0원")
    void dailyRecommendedFlooredAtZero() {
        Budget budget = budget("2000000", "1500000", "600000"); // 월 예산 −100,000

        assertThat(budget.getDailyRecommendedAmount(BigDecimal.ZERO, 25, BigDecimal.ZERO))
                .isEqualByComparingTo("0");
    }
}
