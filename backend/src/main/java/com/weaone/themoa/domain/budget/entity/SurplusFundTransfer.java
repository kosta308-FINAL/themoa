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
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 완료 주기 잉여금을 진행 중인 예산으로 가져온 이력. 원본 {@link SurplusFund}는 당시 주기의 실제 결과로
 * 보존하고, 사용 가능 잔액과 금융상품 추천용 순잉여금은 이 이력 합계를 차감해 계산한다.
 */
@Entity
@Table(name = "surplus_fund_transfer")
@Check(constraints = "amount > 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurplusFundTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private SurplusFundTransfer(Budget budget, BigDecimal amount, LocalDateTime createdAt) {
        this.budget = budget;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static SurplusFundTransfer create(Budget budget, BigDecimal amount, LocalDateTime createdAt) {
        return new SurplusFundTransfer(budget, amount, createdAt);
    }
}
