package com.weaone.themoa.domain.budget.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 급여일 변경 이력(dailyBudget.md §1 후속 범위). 변경 신청 시점이 아니라 "실제로 승격된" 시점에 1행
 * 남긴다 — {@code effectiveCycleStartDate}는 새 payday가 실제로 적용되기 시작한 주기(브리지 주기)의
 * 시작일이다. 이 이력이 있어야 payday·고정지출 매칭·습관 코칭처럼 {@code member.payday}를 직접 읽는
 * 도메인들이 과거 특정 날짜에 "그때 유효했던" payday를 정확히 복원할 수 있다({@code Budget} row가
 * 그 시점에 실제로 생성되어 있지 않은 경우의 대비용).
 */
@Entity
@Table(name = "payday_change_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaydayChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "old_payday", nullable = false)
    private Integer oldPayday;

    @Column(name = "new_payday", nullable = false)
    private Integer newPayday;

    @Column(name = "effective_cycle_start_date", nullable = false)
    private LocalDate effectiveCycleStartDate;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    private PaydayChangeHistory(Member member, Integer oldPayday, Integer newPayday,
                                 LocalDate effectiveCycleStartDate, LocalDateTime changedAt) {
        this.member = member;
        this.oldPayday = oldPayday;
        this.newPayday = newPayday;
        this.effectiveCycleStartDate = effectiveCycleStartDate;
        this.changedAt = changedAt;
    }

    public static PaydayChangeHistory record(Member member, int oldPayday, int newPayday,
                                              LocalDate effectiveCycleStartDate, LocalDateTime changedAt) {
        return new PaydayChangeHistory(member, oldPayday, newPayday, effectiveCycleStartDate, changedAt);
    }
}
