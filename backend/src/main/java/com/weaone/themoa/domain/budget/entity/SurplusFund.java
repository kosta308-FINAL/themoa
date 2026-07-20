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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주기 마감 잉여금(erd.md §6, MOA-S-BUD-BGT-11). {@code amount = 월 예산 − 그 주기 실제 지출}이며
 * 초과지출한 주기는 음수 그대로 저장한다 — 0으로 깎으면 평균 잉여금이 부풀려져 저축 여력 없는 사람에게
 * 적금을 권하게 된다. UNIQUE(member_id, year_month)로 주기당 1행을 보장해 마감 배치 재시도가 중복 적립되지 않는다.
 */
@Entity
@Table(name = "surplus_fund",
        uniqueConstraints = @UniqueConstraint(name = "uk_surplus_fund_member_cycle",
                columnNames = {"member_id", "\"year_month\""}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurplusFund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "\"year_month\"", nullable = false, length = 7)
    private String yearMonth;

    /** 음수 허용(초과지출한 주기). 상품추천 입력값이라 사실 그대로 둔다. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private SurplusFund(Member member, String yearMonth, BigDecimal amount, LocalDate occurredAt,
                        LocalDateTime createdAt) {
        this.member = member;
        this.yearMonth = yearMonth;
        this.amount = amount;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static SurplusFund accrue(Member member, String yearMonth, BigDecimal amount, LocalDate occurredAt,
                                     LocalDateTime createdAt) {
        return new SurplusFund(member, yearMonth, amount, occurredAt, createdAt);
    }
}
