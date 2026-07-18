package com.weaone.themoa.domain.fixedexpense.entity;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * 고정지출 월별 이행 기록(erd.md §5). UNIQUE(fixed_expense_id, year_month)가 "주기당 이행 1회"를 강제하고,
 * UNIQUE(card_transaction_id)가 "거래 1건은 최대 하나의 이행에만 쓰인다"를 강제한다(§5).
 */
@Entity
@Table(name = "fixed_expense_payment",
        uniqueConstraints = @UniqueConstraint(name = "uk_fixed_expense_payment_cycle",
                columnNames = {"fixed_expense_id", "\"year_month\""}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpensePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixed_expense_id", nullable = false)
    private FixedExpense fixedExpense;

    /**
     * 월급 주기 라벨("yyyy-MM"). 월급주기가 없는 사용자는 달력 월로 폴백한다(fixedExpense.md §5).
     * {@code YEAR_MONTH}는 MySQL 예약어(INTERVAL 단위 토큰)라 컬럼명을 그대로 쓰면 DDL이 깨진다 —
     * 큰따옴표로 감싸 Hibernate가 백틱 인용 식별자로 렌더링하게 한다.
     */
    @Column(name = "\"year_month\"", nullable = false, length = 7)
    private String yearMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_transaction_id", unique = true)
    private CardTransaction cardTransaction;

    @Column(name = "paid_amount", precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FixedExpensePaymentStatus status;

    private FixedExpensePayment(FixedExpense fixedExpense, String yearMonth, CardTransaction cardTransaction,
                                 BigDecimal paidAmount, FixedExpensePaymentStatus status) {
        this.fixedExpense = fixedExpense;
        this.yearMonth = yearMonth;
        this.cardTransaction = cardTransaction;
        this.paidAmount = paidAmount;
        this.status = status;
    }

    /** 수집 매칭 성공 시 생성(fixedExpense.md §5). */
    public static FixedExpensePayment paid(FixedExpense fixedExpense, String yearMonth,
                                            CardTransaction cardTransaction, BigDecimal paidAmount) {
        return new FixedExpensePayment(fixedExpense, yearMonth, cardTransaction, paidAmount,
                FixedExpensePaymentStatus.PAID);
    }

    /** 취소 재수집 시 매칭 건이 취소되면 이 행을 삭제한다(미납 복귀, §7) — UNPAID 행을 별도로 만들지 않는다. */
}
