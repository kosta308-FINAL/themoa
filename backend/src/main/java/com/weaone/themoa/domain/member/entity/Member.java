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

import java.math.BigDecimal;
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

    /** 수기→카드 전환 시점(백필 경계, entryMode.md §2-1·§3). MANUAL 상태로 남아 있는 동안은 NULL이다. */
    @Column(name = "card_sync_started_at")
    private LocalDateTime cardSyncStartedAt;

    /** 발급된 Access Token을 만료 전 무효화하는 기준값. 전체 기기 로그아웃·비밀번호 변경에만 올린다. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    /** 현재 월급 원본(dailyBudget.md §1). 소비 가이드 진입 시 지연 수집한다. 주기 스냅샷은 {@code budget.salary_amount}. */
    @Column(name = "salary_amount", precision = 14, scale = 2)
    private BigDecimal salaryAmount;

    /** 월 저축 목표 원본. 미설정이면 예산 계산에서 0으로 본다. 주기 스냅샷은 {@code budget.savings_goal_amount}. */
    @Column(name = "savings_target_amount", precision = 14, scale = 2)
    private BigDecimal savingsTargetAmount;

    /** 명목 월급일(1~31). 없는 날이면 말일로 당긴다. 소비가이드 최초 설정에서만 저장하고 MVP에서 일반 수정은 없다. */
    @Column(name = "payday")
    private Integer payday;

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

    /**
     * 소비 가이드 최초 설정(S-00A, MOA-S-BUD-BGT-01). 월급·급여일을 함께 저장한다. 급여일은 MVP에서
     * 최초 설정 이후 일반 수정 대상이 아니므로(dailyBudget.md §1) 이 메서드로만 채워진다.
     */
    public void configureSpendingGuide(BigDecimal salaryAmount, Integer payday) {
        this.salaryAmount = salaryAmount;
        this.payday = payday;
    }

    /** 월급 원본 변경(MOA-S-BUD-BGT-08). 주기 스냅샷 반영 여부는 호출자가 적용 시점에 따라 결정한다. */
    public void changeSalary(BigDecimal salaryAmount) {
        this.salaryAmount = salaryAmount;
    }

    /** 월 저축 목표 원본 변경(MOA-S-BUD-BGT-03). */
    public void changeSavingsTarget(BigDecimal savingsTargetAmount) {
        this.savingsTargetAmount = savingsTargetAmount;
    }

    /** 소비 가이드는 월급·급여일 둘 다 있어야 진입 가능하다(dailyBudget.md §1). */
    public boolean hasSpendingGuideSetup() {
        return salaryAmount != null && payday != null;
    }

    /** 저축 목표 미설정은 0원으로 계산한다(dayguide.md §2.1). */
    public BigDecimal getSavingsTargetOrZero() {
        return savingsTargetAmount != null ? savingsTargetAmount : BigDecimal.ZERO;
    }

    /**
     * 수기→카드 전환(entryMode.md §2). MANUAL일 때만 CARD로 전이하고 시각을 남긴다. 이미 CARD면 아무 것도
     * 하지 않는다 — 역전이도 재전환도 없어(§2-1) 이 메서드는 평생 최대 1번만 실제 효과를 낸다.
     */
    public void startCardSync(LocalDateTime now) {
        if (entryMode == EntryMode.MANUAL) {
            entryMode = EntryMode.CARD;
            cardSyncStartedAt = now;
        }
    }

    /** 카드 자동수집 재개(entryMode.md §2-1). entry_mode는 건드리지 않는다 — 되돌리는 대상은 이 플래그뿐이다. */
    public void enableCardSync() {
        cardSyncEnabled = true;
    }

    /** "수기로 돌아가기" 요청의 실제 구현(entryMode.md §2-1). entry_mode 역전이가 아니라 이 플래그로 표현한다. */
    public void disableCardSync() {
        cardSyncEnabled = false;
    }

    /** 결제수단=카드인 수기 입력 허용 조건(entryMode.md §5-1): 자동수집이 돌지 않는 동안만 허용한다. */
    public boolean isManualCardEntryAllowed() {
        return entryMode == EntryMode.MANUAL || !cardSyncEnabled;
    }
}