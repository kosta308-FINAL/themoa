package com.weaone.themoa.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    /** 연속 로그인 실패 허용 횟수. 도달하면 {@link #LOGIN_LOCK_DURATION} 동안 잠근다. */
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 식별자. trim + 소문자로 정규화된 값만 저장한다. */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt 해시. 소셜 전용 회원은 NULL이다(가짜 비밀번호를 만들지 않는다). */
    @Column(length = 100)
    private String password;

    /** 닉네임. 실명이 아니다. */
    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_mode", nullable = false, length = 10)
    private EntryMode entryMode;

    @Column(name = "card_sync_enabled", nullable = false)
    private boolean cardSyncEnabled;

    /** 발급된 Access Token을 만료 전 무효화하는 기준값. 전체 기기 로그아웃·비밀번호 변경에만 올린다. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    private Member(String email, String password, String name, Gender gender, LocalDate birthDate) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.entryMode = EntryMode.MANUAL;
        this.cardSyncEnabled = true;
        this.tokenVersion = 0;
        this.loginFailCount = 0;
    }

    /** 일반 가입 회원. 비밀번호는 해시만 받는다. */
    public static Member signUp(String email, String passwordHash, String name, Gender gender, LocalDate birthDate) {
        return new Member(email, passwordHash, name, gender, birthDate);
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** 잠금 시간이 지났으면 자동 해제한다. 로그인 시도 시점에 호출한다. */
    public void releaseLockIfExpired(LocalDateTime now) {
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            lockedUntil = null;
            loginFailCount = 0;
        }
    }

    public void recordLoginFailure(LocalDateTime now) {
        loginFailCount++;
        if (loginFailCount >= MAX_LOGIN_FAIL_COUNT) {
            lockedUntil = now.plus(LOGIN_LOCK_DURATION);
        }
    }

    public void recordLoginSuccess(LocalDateTime now) {
        loginFailCount = 0;
        lockedUntil = null;
        lastActiveAt = now;
    }

    /** 전체 기기 로그아웃·비밀번호 변경 전용. 올리면 이 회원의 모든 기기 Access Token이 즉시 무효가 된다. */
    public void increaseTokenVersion() {
        tokenVersion++;
    }

    /** 비밀번호 변경(auth.md §7-3). token_version 증가·세션 폐기는 호출자(AuthService)가 이어서 처리한다. */
    public void changePassword(String newPasswordHash) {
        this.password = newPasswordHash;
    }

    /**
     * last_active_at만 갱신한다(로그인 실패 카운트·잠금 상태는 건드리지 않음). 30일 초과 복귀 동기화
     * 선택 완료 시점처럼, 로그인 자체와는 다른 "실제 이용" 신호에 사용한다(cardtransaction.md §6-C).
     */
    public void markActive(LocalDateTime now) {
        this.lastActiveAt = now;
    }

    public boolean isReturningAfterAbsence(LocalDateTime now, long absenceDays) {
        return lastActiveAt != null && lastActiveAt.isBefore(now.minusDays(absenceDays));
    }
}