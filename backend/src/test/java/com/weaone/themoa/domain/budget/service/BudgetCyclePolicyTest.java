package com.weaone.themoa.domain.budget.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 급여일 기준 주기 계산(dailyBudget.md §1, MOA-S-BUD-BGT-02). */
class BudgetCyclePolicyTest {

    @Test
    @DisplayName("급여일 5일·오늘 7/11이면 주기는 7/5~8/4, 라벨은 2026-07")
    void cycleWithinMonth() {
        BudgetCyclePolicy.BudgetCycle cycle = BudgetCyclePolicy.cycleOf(5, LocalDate.of(2026, 7, 11));

        assertThat(cycle.cycleStartDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(cycle.cycleEndDate()).isEqualTo(LocalDate.of(2026, 8, 4));
        assertThat(cycle.yearMonth()).isEqualTo("2026-07");
    }

    @Test
    @DisplayName("급여일 5일·오늘이 급여일 당일(7/5)이면 그날부터 시작한다")
    void cycleStartsOnPaydayItself() {
        BudgetCyclePolicy.BudgetCycle cycle = BudgetCyclePolicy.cycleOf(5, LocalDate.of(2026, 7, 5));

        assertThat(cycle.cycleStartDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(cycle.cycleEndDate()).isEqualTo(LocalDate.of(2026, 8, 4));
    }

    @Test
    @DisplayName("급여일 5일·오늘 7/3이면 아직 이번 달 급여 전이라 직전 주기(6/5~7/4)")
    void cycleBeforePayday() {
        BudgetCyclePolicy.BudgetCycle cycle = BudgetCyclePolicy.cycleOf(5, LocalDate.of(2026, 7, 3));

        assertThat(cycle.cycleStartDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(cycle.cycleEndDate()).isEqualTo(LocalDate.of(2026, 7, 4));
        assertThat(cycle.yearMonth()).isEqualTo("2026-06");
    }

    @Test
    @DisplayName("급여일 31일은 없는 달엔 말일로 당긴다 — 2월 주기는 2/28 시작")
    void clampsToShortMonth() {
        BudgetCyclePolicy.BudgetCycle cycle = BudgetCyclePolicy.cycleOf(31, LocalDate.of(2026, 3, 1));

        // 2026-02는 28일까지 → 2/28 시작, 다음 시작은 3/31이라 종료는 3/30
        assertThat(cycle.cycleStartDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(cycle.cycleEndDate()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(cycle.yearMonth()).isEqualTo("2026-02");
    }

    @Test
    @DisplayName("남은 일수는 오늘 포함 — 7/11~8/4는 25일, 종료일 당일은 1일")
    void remainingDaysIncludesToday() {
        assertThat(BudgetCyclePolicy.remainingDays(LocalDate.of(2026, 7, 11), LocalDate.of(2026, 8, 4)))
                .isEqualTo(25);
        assertThat(BudgetCyclePolicy.remainingDays(LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4)))
                .isEqualTo(1);
    }
}
