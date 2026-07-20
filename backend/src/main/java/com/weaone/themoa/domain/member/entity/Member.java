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

    /** 고객센터 관리자 API 인가(customerservice.md §3-2). 가입 요청으로 받지 않으며 전 회원 기본값은 USER다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.USER;

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

    /** 소득유형(SALARY=고정 월급, HOURLY=시급제). 최초 설정 이후에도 예산 기준 화면에서 전환할 수 있다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "income_type", nullable = false, length = 10)
    private IncomeType incomeType = IncomeType.SALARY;

    /** 현재 월급 원본(dailyBudget.md §1). 소비 가이드 진입 시 지연 수집한다. 주기 스냅샷은 {@code budget.salary_amount}. incomeType=SALARY일 때만 쓴다. */
    @Column(name = "salary_amount", precision = 14, scale = 2)
    private BigDecimal salaryAmount;

    /** 시급(incomeType=HOURLY 전용). 요일별 근무시간과 곱해 급여주기마다 예상 소득을 산출한다. */
    @Column(name = "hourly_wage", precision = 14, scale = 2)
    private BigDecimal hourlyWage;

    /** 월 저축 목표 원본. 미설정이면 예산 계산에서 0으로 본다. 주기 스냅샷은 {@code budget.savings_goal_amount}. */
    @Column(name = "savings_target_amount", precision = 14, scale = 2)
    private BigDecimal savingsTargetAmount;

    /** 명목 월급일(1~31). 없는 날이면 말일로 당긴다. 소비가이드 최초 설정에서 저장한다. */
    @Column(name = "payday")
    private Integer payday;

    /**
     * 급여일 변경 예약값. 사용자가 급여일을 바꾸면 이 필드만 채워지고 {@link #payday}는 그대로 둔다 —
     * 진행 중인 주기는 항상 기존 payday로 계산돼야 하기 때문이다(dailyBudget.md §1). 다음 주기가 열리는
     * 순간(그 주기의 {@code Budget} row가 아직 없는 시점) {@link #applyPendingPayday()}로 승격된다.
     */
    @Column(name = "pending_payday")
    private Integer pendingPayday;

    /** 가입일(dayguide.md §3.4·§8): 수기 모드 사용자의 카테고리 도넛 주기 이동 하한 계산에 쓰인다. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Member(String email, String password, String name, Gender gender, LocalDate birthDate, LocalDateTime now) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.entryMode = EntryMode.MANUAL;
        this.cardSyncEnabled = true;
        this.tokenVersion = 0;
        this.loginFailCount = 0;
        this.createdAt = now;
    }

    /** 일반 가입 회원. 비밀번호는 해시만 받는다. */
    public static Member signUp(String email, String passwordHash, String name, Gender gender, LocalDate birthDate,
                                 LocalDateTime now) {
        return new Member(email, passwordHash, name, gender, birthDate, now);
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

    /**
     * 관리자 지정·해제(erd.md §1). 운영자가 DB에서 직접 처리하는 범위라 별도 API는 없다.
     * role 클레임이 담긴 기존 Access Token을 즉시 무효화하기 위해 token_version도 함께 올린다.
     */
    public void changeRole(Role role) {
        this.role = role;
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
     * 소비 가이드 최초 설정(S-00A, MOA-S-BUD-BGT-01). 월급·급여일을 함께 저장한다.
     *
     * <p>incomeType=SALARY면 salaryAmount, HOURLY면 hourlyWage를 채운다(요일별 근무시간 행 저장은
     * 호출자가 같은 트랜잭션에서 별도로 처리한다 — Member는 소득유형·시급만 안다).
     */
    public void configureSpendingGuide(IncomeType incomeType, BigDecimal salaryAmount, BigDecimal hourlyWage,
                                        Integer payday) {
        this.incomeType = incomeType;
        this.salaryAmount = salaryAmount;
        this.hourlyWage = hourlyWage;
        this.payday = payday;
    }

    /**
     * 급여일 변경 신청. 즉시 반영하지 않고 예약만 한다 — 실제 적용은 다음 주기가 열릴 때
     * BudgetCycleService가 {@link #applyPendingPayday()}를 호출해 승격한다.
     */
    public void requestPaydayChange(int newPayday) {
        this.pendingPayday = newPayday;
    }

    /** 예약된 급여일을 실제 급여일로 승격한다. 호출 전 {@code pendingPayday != null}을 보장해야 한다. */
    public void applyPendingPayday() {
        this.payday = this.pendingPayday;
        this.pendingPayday = null;
    }

    /** 월급 원본 변경(MOA-S-BUD-BGT-08, incomeType=SALARY 전용). 주기 스냅샷 반영 여부는 호출자가 결정한다. */
    public void changeSalary(BigDecimal salaryAmount) {
        this.salaryAmount = salaryAmount;
    }

    /** 시급 변경(incomeType=HOURLY 전용). 요일별 근무시간 행 교체는 호출자가 같은 트랜잭션에서 처리한다. */
    public void changeHourlyWage(BigDecimal hourlyWage) {
        this.hourlyWage = hourlyWage;
    }

    /** 월 저축 목표 원본 변경(MOA-S-BUD-BGT-03). */
    public void changeSavingsTarget(BigDecimal savingsTargetAmount) {
        this.savingsTargetAmount = savingsTargetAmount;
    }

    /** 소비 가이드는 급여일과, 소득유형에 맞는 소득 입력(월급 또는 시급)이 있어야 진입 가능하다(dailyBudget.md §1). */
    public boolean hasSpendingGuideSetup() {
        if (payday == null) {
            return false;
        }
        return incomeType == IncomeType.HOURLY ? hourlyWage != null : salaryAmount != null;
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