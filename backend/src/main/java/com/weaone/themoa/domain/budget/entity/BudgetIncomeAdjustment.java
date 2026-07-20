package com.weaone.themoa.domain.budget.entity;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * "수입 직접 입력"(알림.md:200 "비정기 수입은 별도 추가 예산 기능으로 처리"의 구체화). 용돈·정부지원금 같은
 * 비정기 수입이나 알바 예정 근무시간과 실제의 차액 보정을 그 주기 {@code budget}에 더한다.
 *
 * <p>의도적으로 {@code card_transaction}(수기 거래 포함)과 분리된 테이블이다 — 순지출·카테고리 집계·
 * 소비내역 히스토리는 전부 "차감 전제"로 짜여 있어(erd.md §4 card_transaction), 여기에 수입을 섞으면
 * 그 모든 소비 통계 로직을 다시 손봐야 한다. 대신 이 값은 {@code budget}의 사용가능금액 계산 한 곳에만
 * 더해지고, 소비 집계에는 전혀 반영되지 않는다. 금액은 양수(추가 수입)·음수(예정보다 덜 번 차액 보정)
 * 둘 다 허용하되 0은 서비스 레벨에서 거부한다.
 */
@Entity
@Table(name = "budget_income_adjustment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetIncomeAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String memo;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private BudgetIncomeAdjustment(Budget budget, BigDecimal amount, String memo, LocalDate occurredAt,
                                    LocalDateTime createdAt) {
        this.budget = budget;
        this.amount = amount;
        this.memo = memo;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static BudgetIncomeAdjustment create(Budget budget, BigDecimal amount, String memo, LocalDate occurredAt,
                                                 LocalDateTime createdAt) {
        return new BudgetIncomeAdjustment(budget, amount, memo, occurredAt, createdAt);
    }
}
