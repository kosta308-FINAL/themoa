package com.weaone.themoa.domain.subscription.entity;

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

/**
 * 가입한 상품의 우대조건 항목(체크리스트). "이 조건을 지켜야 우대금리를 받는다"를 관리하는 용도다.
 *
 * <p>{@code met}은 사용자가 현재 그 조건을 충족하고 있는지다. 충족을 놓치면 만기 때 금리가 깎이므로,
 * 대시보드에서 미충족 항목을 강조해 사용자가 챙기게 한다.
 */
@Entity
@Table(name = "savings_subscription_condition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingsSubscriptionCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SavingsSubscription subscription;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    /** 이 조건이 주는 가산금리(%p). 사용자가 확정한 값. */
    @Column(name = "rate_bonus", precision = 5, scale = 2)
    private BigDecimal rateBonus;

    /** 현재 이 조건을 충족하고 있는지(가입 시 체크한 값, 이후 토글 가능). */
    @Column(name = "met", nullable = false)
    private boolean met;

    private SavingsSubscriptionCondition(String description, BigDecimal rateBonus, boolean met) {
        this.description = description;
        this.rateBonus = rateBonus;
        this.met = met;
    }

    public static SavingsSubscriptionCondition of(String description, BigDecimal rateBonus, boolean met) {
        return new SavingsSubscriptionCondition(description, rateBonus, met);
    }

    void assignTo(SavingsSubscription subscription) {
        this.subscription = subscription;
    }

    public void updateMet(boolean met) {
        this.met = met;
    }
}
