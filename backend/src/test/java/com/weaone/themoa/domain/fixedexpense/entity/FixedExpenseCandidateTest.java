package com.weaone.themoa.domain.fixedexpense.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** fixedExpense.md §3 후보 상태머신(재추천 방지) 검증. */
class FixedExpenseCandidateTest {

    private FixedExpenseCandidate candidate() {
        return FixedExpenseCandidate.create(null, null, null, BigDecimal.valueOf(3), "매달 7,890원 나가는 쿠팡와우, 등록할까요?");
    }

    @Test
    @DisplayName("최초 생성은 PENDING 상태로 시작한다")
    void createStartsPending() {
        FixedExpenseCandidate candidate = candidate();

        assertThat(candidate.getStatus()).isEqualTo(FixedExpenseCandidateStatus.PENDING);
        assertThat(candidate.getSnoozedYearMonth()).isNull();
    }

    @Test
    @DisplayName("나중에(snooze)는 현재 주기를 snoozedYearMonth에 기록하고 EXCLUDED_THIS_MONTH로 전환한다")
    void snoozeRecordsCurrentCycle() {
        FixedExpenseCandidate candidate = candidate();

        candidate.snooze("2026-07");

        assertThat(candidate.getStatus()).isEqualTo(FixedExpenseCandidateStatus.EXCLUDED_THIS_MONTH);
        assertThat(candidate.getSnoozedYearMonth()).isEqualTo("2026-07");
    }

    @Test
    @DisplayName("현재 주기가 스누즈 주기와 같으면 아직 스누즈가 유효하다")
    void snoozeNotExpiredWithinSameCycle() {
        FixedExpenseCandidate candidate = candidate();
        candidate.snooze("2026-07");

        assertThat(candidate.isSnoozeExpired("2026-07")).isFalse();
    }

    @Test
    @DisplayName("현재 주기가 스누즈 주기를 넘어서면 재추천 대상이다")
    void snoozeExpiredAfterNextCycle() {
        FixedExpenseCandidate candidate = candidate();
        candidate.snooze("2026-07");

        assertThat(candidate.isSnoozeExpired("2026-08")).isTrue();
    }

    @Test
    @DisplayName("스누즈 만료 후 reopen하면 PENDING으로 돌아가고 snoozedYearMonth가 지워진다")
    void reopenClearsSnooze() {
        FixedExpenseCandidate candidate = candidate();
        candidate.snooze("2026-07");

        candidate.reopen();

        assertThat(candidate.getStatus()).isEqualTo(FixedExpenseCandidateStatus.PENDING);
        assertThat(candidate.getSnoozedYearMonth()).isNull();
    }

    @Test
    @DisplayName("거절하면 DO_NOT_RECOMMEND로 영구 전환된다")
    void rejectIsPermanent() {
        FixedExpenseCandidate candidate = candidate();

        candidate.reject();

        assertThat(candidate.getStatus()).isEqualTo(FixedExpenseCandidateStatus.DO_NOT_RECOMMEND);
    }

    @Test
    @DisplayName("습관적 소비로 분류하면 CLASSIFIED_HABIT으로 전환된다")
    void classifyHabitTransitionsStatus() {
        FixedExpenseCandidate candidate = candidate();

        candidate.classifyHabit();

        assertThat(candidate.getStatus()).isEqualTo(FixedExpenseCandidateStatus.CLASSIFIED_HABIT);
    }

    @Test
    @DisplayName("등록하면 REGISTERED로 전환된다")
    void registerTransitionsStatus() {
        FixedExpenseCandidate candidate = candidate();

        candidate.register();

        assertThat(candidate.getStatus()).isEqualTo(FixedExpenseCandidateStatus.REGISTERED);
    }

    @Test
    @DisplayName("refresh는 추천 내용만 갱신하고 상태는 건드리지 않는다")
    void refreshDoesNotChangeStatus() {
        FixedExpenseCandidate candidate = candidate();
        candidate.snooze("2026-07");

        candidate.refresh(null, BigDecimal.valueOf(4), "매달 8,000원 나가는 쿠팡와우, 등록할까요?");

        assertThat(candidate.getStatus()).isEqualTo(FixedExpenseCandidateStatus.EXCLUDED_THIS_MONTH);
        assertThat(candidate.getScore()).isEqualByComparingTo("4");
        assertThat(candidate.getRecommendMessage()).isEqualTo("매달 8,000원 나가는 쿠팡와우, 등록할까요?");
    }
}
