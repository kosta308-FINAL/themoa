package com.weaone.themoa.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 13, 10, 0);

    private Member member() {
        return Member.signUp("user@example.com", "hashed", "닉네임", Gender.FEMALE, LocalDate.of(2000, 1, 1));
    }

    @Test
    @DisplayName("연속 5회 실패하면 15분간 잠긴다")
    void locksAfterFiveFailures() {
        Member member = member();

        for (int i = 0; i < 5; i++) {
            member.recordLoginFailure(NOW);
        }

        assertThat(member.isLocked(NOW)).isTrue();
        assertThat(member.isLocked(NOW.plusMinutes(14))).isTrue();
        assertThat(member.isLocked(NOW.plusMinutes(15))).isFalse();
    }

    @Test
    @DisplayName("4회 실패까지는 잠기지 않는다")
    void notLockedBeforeThreshold() {
        Member member = member();

        for (int i = 0; i < 4; i++) {
            member.recordLoginFailure(NOW);
        }

        assertThat(member.isLocked(NOW)).isFalse();
    }

    @Test
    @DisplayName("잠금 시간이 지나면 실패 횟수까지 초기화된다")
    void releasesExpiredLock() {
        Member member = member();
        for (int i = 0; i < 5; i++) {
            member.recordLoginFailure(NOW);
        }

        LocalDateTime afterLock = NOW.plusMinutes(16);
        member.releaseLockIfExpired(afterLock);

        assertThat(member.isLocked(afterLock)).isFalse();
        assertThat(member.getLoginFailCount()).isZero();

        // 해제 직후 1회 실패해도 다시 잠기지 않는다(카운트가 1부터 다시 쌓인다).
        member.recordLoginFailure(afterLock);
        assertThat(member.isLocked(afterLock)).isFalse();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 횟수·잠금이 풀리고 최종 이용시각이 갱신된다")
    void resetsOnSuccess() {
        Member member = member();
        member.recordLoginFailure(NOW);
        member.recordLoginFailure(NOW);

        member.recordLoginSuccess(NOW);

        assertThat(member.getLoginFailCount()).isZero();
        assertThat(member.getLockedUntil()).isNull();
        assertThat(member.getLastActiveAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("가입 회원은 수기 입력 모드와 토큰 버전 0으로 시작한다")
    void signUpDefaults() {
        Member member = member();

        assertThat(member.getEntryMode()).isEqualTo(EntryMode.MANUAL);
        assertThat(member.isCardSyncEnabled()).isTrue();
        assertThat(member.getTokenVersion()).isZero();
    }
}