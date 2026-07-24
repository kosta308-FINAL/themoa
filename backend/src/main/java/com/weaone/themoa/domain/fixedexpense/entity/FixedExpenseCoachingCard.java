package com.weaone.themoa.domain.fixedexpense.entity;

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
import java.time.LocalDateTime;

/**
 * 고정지출 연 환산 코칭 카드. 절감을 종용하지 않는다 — "월세/관리비/보험처럼 필수 성격의 지출은 대상에서
 * 빼고, 구독·여가성처럼 재량 조정 가능한 항목만 담담하게 "연으로 보면 얼마"만 알려준다. 대상 선정(어떤
 * 고정지출을 보여줄지)은 LLM이 이름·카테고리를 보고 판단하고, 금액은 항상 규칙 계층이 계산한 값만 쓴다.
 */
@Entity
@Table(name = "fixed_expense_coaching_card",
        uniqueConstraints = @UniqueConstraint(name = "uk_fx_coaching_card_display_order",
                columnNames = {"member_id", "\"year_month\"", "display_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpenseCoachingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 카드가 만들어진 근거 급여 주기 라벨. {@code year_month}는 MySQL 예약어라 인용한다. */
    @Column(name = "\"year_month\"", nullable = false, length = 7)
    private String yearMonth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixed_expense_id", nullable = false)
    private FixedExpense fixedExpense;

    /** 문장은 전부 AI(또는 폴백 템플릿)가 쓴다. */
    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** 연 환산 금액. 규칙 계층이 {@code expectedAmountKrw * 12}로 계산한다 — LLM 반환값을 쓰지 않는다. */
    @Column(name = "annual_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal annualAmount;

    /** 주기 내 표시 순서 1~3. */
    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 사용자가 이 카드를 넘긴 시각. null이면 이번 주기 목록에 그대로 노출된다. */
    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    private FixedExpenseCoachingCard(Member member, String yearMonth, FixedExpense fixedExpense, String title,
                                      String body, BigDecimal annualAmount, short displayOrder,
                                      LocalDateTime createdAt) {
        this.member = member;
        this.yearMonth = yearMonth;
        this.fixedExpense = fixedExpense;
        this.title = title;
        this.body = body;
        this.annualAmount = annualAmount;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }

    public static FixedExpenseCoachingCard of(Member member, String yearMonth, FixedExpense fixedExpense,
                                               String title, String body, BigDecimal annualAmount,
                                               short displayOrder, LocalDateTime createdAt) {
        return new FixedExpenseCoachingCard(member, yearMonth, fixedExpense, title, body, annualAmount, displayOrder,
                createdAt);
    }

    public void markDismissed(LocalDateTime now) {
        this.dismissedAt = now;
    }
}
