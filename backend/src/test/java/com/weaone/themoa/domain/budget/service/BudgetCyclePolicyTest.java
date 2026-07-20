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

    @Test
    @DisplayName("급여일을 뒤로 미루면(5일→20일) 브리지 주기는 이전 주기 종료 다음날부터 새 급여일 전날까지")
    void bridgeCycleWhenPaydayMovesLater() {
        // 이전 주기(급여일 5일 기준)가 8/4에 끝났다고 가정 — 다음날(8/5)부터 새 급여일 20일 도래 전날(8/19)까지
        BudgetCyclePolicy.BudgetCycle bridge =
                BudgetCyclePolicy.bridgeCycle(LocalDate.of(2026, 8, 4), 20);

        assertThat(bridge.cycleStartDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(bridge.cycleEndDate()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(bridge.yearMonth()).isEqualTo("2026-08");
        // 다음 주기는 브리지 종료 다음날(8/20)부터 다시 cycleOf만으로 이어진다
        BudgetCyclePolicy.BudgetCycle next = BudgetCyclePolicy.cycleOf(20, bridge.cycleEndDate().plusDays(1));
        assertThat(next.cycleStartDate()).isEqualTo(bridge.cycleEndDate().plusDays(1));
    }

    @Test
    @DisplayName("급여일을 앞당기면(20일→5일) 겹치지 않고 이전 주기 종료 다음날부터 다음 달 5일 전날까지로 이어붙인다")
    void bridgeCycleWhenPaydayMovesEarlier() {
        // 이전 주기(급여일 20일 기준)가 8/19에 끝났다고 가정 — 새 급여일 5일은 이미 지나있어 다음 달 5일이 첫 도래일
        BudgetCyclePolicy.BudgetCycle bridge =
                BudgetCyclePolicy.bridgeCycle(LocalDate.of(2026, 8, 19), 5);

        assertThat(bridge.cycleStartDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(bridge.cycleEndDate()).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(bridge.yearMonth()).isEqualTo("2026-08");
    }

    @Test
    @DisplayName("이전 주기 종료 다음날이 마침 새 급여일 당일이면 그 다음 달 급여일까지 정상 길이 주기가 된다")
    void bridgeCycleWhenStartLandsExactlyOnNewPayday() {
        BudgetCyclePolicy.BudgetCycle bridge =
                BudgetCyclePolicy.bridgeCycle(LocalDate.of(2026, 8, 4), 5);

        assertThat(bridge.cycleStartDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(bridge.cycleEndDate()).isEqualTo(LocalDate.of(2026, 9, 4));
    }
}
