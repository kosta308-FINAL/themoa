package com.weaone.themoa.domain.subscription.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자가 실제로 가입한 예·적금. 사용자 입력에 의존해 등록한다(자동 연동이 아니라 본인이 등록).
 *
 * <p>상품명·회사명은 스냅샷으로 함께 저장한다. 나중에 상품이 판매종료되거나 정보가 바뀌어도 내 가입 기록은
 * 가입 당시 그대로 보여야 하기 때문이다. 적용금리도 우대조건 텍스트에서 자동 계산한 값이 아니라 사용자가
 * 확정한 값(실제 은행에서 안내받은 금리)을 저장한다.
 */
@Entity
@Table(
        name = "savings_subscription",
        indexes = @Index(name = "idx_savings_subscription_member", columnList = "member_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingsSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 원본 상품 id(savings_product.id). 상품이 사라져도 기록은 유지되므로 FK는 걸지 않는다. */
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    /** 예금/적금 구분 스냅샷(DEPOSIT/SAVING). */
    @Column(name = "product_type", length = 20)
    private String productType;

    /** 월 납입액(원). 정기예금처럼 목돈 예치면 예치금액을 담는다. */
    @Column(name = "monthly_amount", nullable = false)
    private Long monthlyAmount;

    /** 사용자가 확정한 적용금리(연 %). 기본금리 + 체크한 우대조건 합계를 사용자가 확인·수정한 값. */
    @Column(name = "applied_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal appliedRate;

    @Column(name = "term_month", nullable = false)
    private int termMonth;

    /** 단리/복리(만기 예상금액 계산에 쓴다). true면 복리. */
    @Column(name = "compound", nullable = false)
    private boolean compound;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavingsSubscriptionCondition> conditions = new ArrayList<>();

    private SavingsSubscription(Member member, Long productId, String productName, String companyName,
                               String productType, Long monthlyAmount, BigDecimal appliedRate, int termMonth,
                               boolean compound, LocalDate startDate, LocalDate maturityDate, LocalDateTime now) {
        this.member = member;
        this.productId = productId;
        this.productName = productName;
        this.companyName = companyName;
        this.productType = productType;
        this.monthlyAmount = monthlyAmount;
        this.appliedRate = appliedRate;
        this.termMonth = termMonth;
        this.compound = compound;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.createdAt = now;
    }

    public static SavingsSubscription create(Member member, Long productId, String productName, String companyName,
                                             String productType, Long monthlyAmount, BigDecimal appliedRate,
                                             int termMonth, boolean compound, LocalDate startDate,
                                             LocalDateTime now) {
        return new SavingsSubscription(member, productId, productName, companyName, productType, monthlyAmount,
                appliedRate, termMonth, compound, startDate, startDate.plusMonths(termMonth), now);
    }

    public void addCondition(SavingsSubscriptionCondition condition) {
        this.conditions.add(condition);
        condition.assignTo(this);
    }
}
