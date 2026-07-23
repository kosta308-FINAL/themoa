package com.weaone.themoa.domain.fixedexpense.entity;

import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
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
import java.time.LocalDate;

/**
 * 반복결제 그룹(fixedExpense.md §2). 이름형은 alias 레벨로 그룹핑한다 — merchant 단위면 표기 흔들림으로
 * 그룹이 쪼개진다. biller(Apple 등) 경유 결제는 카드 거래에 merchant_alias_id가 붙지 않으므로
 * {@code billerMerchant} + 금액 클러스터링으로 그룹핑한다(merchant.md §5-D-3,
 * {@link FixedExpenseDetectionService}, troubleshooting/billerProblem.md).
 *
 * <p>회원+alias당 그룹 1개로 제한하지 않는다(UNIQUE 없음) — 같은 alias 아래 서로 다른 구독이
 * 공존할 수 있어서다(예: 같은 서비스를 계정 두 개로 구독). biller형과 마찬가지로 find-or-create는
 * 애플리케이션 레벨({@link FixedExpenseDetectionService})이 담당하며, 새벽 배치 단일 라이터라
 * 동시 경합 위험이 낮다.
 */
@Entity
@Table(name = "recurring_payment_group")
@Check(constraints = "(merchant_alias_id is not null) <> (biller_merchant_id is not null)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringPaymentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 이름형 그룹만 채운다. biller형 그룹은 NULL — 후보 승인 시점에야 사용자가 이름을 짓는다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    /** biller형 그룹만 채운다(어느 결제대행사를 거쳤는지). 이름형 그룹은 NULL. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_merchant_id")
    private Merchant billerMerchant;

    @Column(name = "repeat_count", nullable = false)
    private short repeatCount;

    @Column(name = "avg_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal avgAmount;

    @Column(name = "amount_variance_pct", precision = 5, scale = 2)
    private BigDecimal amountVariancePct;

    @Column(name = "avg_pay_day", nullable = false)
    private short avgPayDay;

    @Column(name = "pay_day_variance")
    private Short payDayVariance;

    @Column(name = "first_detected_at", nullable = false)
    private LocalDate firstDetectedAt;

    @Column(name = "last_detected_at", nullable = false)
    private LocalDate lastDetectedAt;

    private RecurringPaymentGroup(Member member, MerchantAlias merchantAlias, Merchant billerMerchant,
                                   short repeatCount, BigDecimal avgAmount, BigDecimal amountVariancePct,
                                   short avgPayDay, Short payDayVariance, LocalDate detectedDate) {
        this.member = member;
        this.merchantAlias = merchantAlias;
        this.billerMerchant = billerMerchant;
        this.repeatCount = repeatCount;
        this.avgAmount = avgAmount;
        this.amountVariancePct = amountVariancePct;
        this.avgPayDay = avgPayDay;
        this.payDayVariance = payDayVariance;
        this.firstDetectedAt = detectedDate;
        this.lastDetectedAt = detectedDate;
    }

    /** 이름형 그룹 탐지. */
    public static RecurringPaymentGroup detect(Member member, MerchantAlias merchantAlias, short repeatCount,
                                                 BigDecimal avgAmount, BigDecimal amountVariancePct,
                                                 short avgPayDay, Short payDayVariance, LocalDate detectedDate) {
        return new RecurringPaymentGroup(member, merchantAlias, null, repeatCount, avgAmount, amountVariancePct,
                avgPayDay, payDayVariance, detectedDate);
    }

    /** biller형 그룹 탐지(금액 버킷 단위). merchant.md §5-D-3. */
    public static RecurringPaymentGroup detectBiller(Member member, Merchant billerMerchant, short repeatCount,
                                                       BigDecimal avgAmount, BigDecimal amountVariancePct,
                                                       short avgPayDay, Short payDayVariance, LocalDate detectedDate) {
        return new RecurringPaymentGroup(member, null, billerMerchant, repeatCount, avgAmount, amountVariancePct,
                avgPayDay, payDayVariance, detectedDate);
    }

    /** 재탐지 갱신(새벽 배치 재실행). 최초 탐지일·신원(alias/biller)은 바꾸지 않는다. */
    public void updateStats(short repeatCount, BigDecimal avgAmount, BigDecimal amountVariancePct,
                             short avgPayDay, Short payDayVariance, LocalDate detectedDate) {
        this.repeatCount = repeatCount;
        this.avgAmount = avgAmount;
        this.amountVariancePct = amountVariancePct;
        this.avgPayDay = avgPayDay;
        this.payDayVariance = payDayVariance;
        this.lastDetectedAt = detectedDate;
    }

    public boolean isBillerGroup() {
        return billerMerchant != null;
    }
}
