package com.weaone.themoa.domain.cardconnection.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionAttemptTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 14, 10, 0);
    private static final Duration COOLDOWN = Duration.ofMinutes(5);
    private static final int MAX_FAIL_COUNT = 3;

    private ConnectionAttempt attempt() {
        return ConnectionAttempt.start(null, null);
    }

    @Test
    @DisplayName("연속 3회 실패하면 쿨다운이 걸린다")
    void locksAfterThreeFailures() {
        ConnectionAttempt attempt = attempt();

        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);
        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);
        assertThat(attempt.isCoolingDown(NOW)).isFalse();

        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);
        assertThat(attempt.isCoolingDown(NOW)).isTrue();
        assertThat(attempt.isCoolingDown(NOW.plusMinutes(4))).isTrue();
        assertThat(attempt.isCoolingDown(NOW.plusMinutes(5))).isFalse();
    }

    @Test
    @DisplayName("쿨다운이 지나면 실패 횟수까지 초기화된다")
    void releasesExpiredCooldown() {
        ConnectionAttempt attempt = attempt();
        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);
        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);
        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);

        LocalDateTime afterCooldown = NOW.plusMinutes(6);
        attempt.releaseCooldownIfExpired(afterCooldown);

        assertThat(attempt.isCoolingDown(afterCooldown)).isFalse();
        assertThat(attempt.getFailCount()).isZero();

        // 해제 직후 1회 실패해도 다시 쿨다운이 걸리지 않는다(카운트가 1부터 다시 쌓인다).
        attempt.recordFailure(afterCooldown, MAX_FAIL_COUNT, COOLDOWN);
        assertThat(attempt.isCoolingDown(afterCooldown)).isFalse();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 횟수와 쿨다운이 모두 리셋된다")
    void resetsOnSuccess() {
        ConnectionAttempt attempt = attempt();
        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);
        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);
        attempt.recordFailure(NOW, MAX_FAIL_COUNT, COOLDOWN);

        attempt.reset();

        assertThat(attempt.getFailCount()).isZero();
        assertThat(attempt.isCoolingDown(NOW)).isFalse();
    }
}
