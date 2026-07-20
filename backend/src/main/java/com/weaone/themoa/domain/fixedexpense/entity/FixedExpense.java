package com.weaone.themoa.domain.fixedexpense.entity;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 등록된 고정지출(fixedExpense.md §4·§5, erd.md §5). 두 경로로 생긴다 — 후보 승인({@link #fromCandidate})은
 * 카드형으로 고정되고, 직접 등록({@link #registerDirect})은 결제수단을 사용자가 고른다.
 *
 * <p>{@code card_id}는 erd.md 확정 스키마엔 있지만 등록 화면(F-03)이 특정 카드를 고르게 하지 않아
 * 이번 구현에서는 채우지 않는다(항상 NULL) — "어느 카드로 내는지"까지 추적하는 기능이 생기면 그때 채운다.
 */
@Entity
@Table(name = "fixed_expense")
@Check(constraints = "payment_method <> 'CARD' OR merchant_alias_id IS NOT NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** NULL = 직접 등록(경로 B). 후보 1건당 고정지출은 최대 1건이라 UNIQUE다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", unique = true)
    private FixedExpenseCandidate candidate;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** 무엇을 구독하나(웨이브). payment_method=CARD면 CHECK로 NOT NULL 강제, TRANSFER만 NULL 허용. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    /** 어디로 청구되나(Apple 등). biller 경유 구독만 채운다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_merchant_id")
    private Merchant billerMerchant;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 10)
    private FixedExpensePaymentMethod paymentMethod;

    @Column(name = "expected_pay_day")
    private Short expectedPayDay;

    /** 매칭 전용. 국내=원화, 해외=외화 원금. 예산 차감에는 절대 쓰지 않는다(§5). */
    @Column(name = "expected_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "expected_currency", nullable = false, length = 3)
    private String expectedCurrency;

    /** 예산 차감 전용 원화 스냅샷. 국내는 expectedAmount와 동일, 해외는 환산값. */
    @Column(name = "expected_amount_krw", nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedAmountKrw;

    @Column(name = "krw_converted_date")
    private LocalDate krwConvertedDate;

    @Column(name = "krw_exchange_rate", precision = 10, scale = 4)
    private BigDecimal krwExchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FixedExpenseStatus status;

    private FixedExpense(Member member, FixedExpenseCandidate candidate, String name, Category category,
                          MerchantAlias merchantAlias, Merchant billerMerchant, FixedExpensePaymentMethod paymentMethod,
                          Short expectedPayDay, BigDecimal expectedAmount, String expectedCurrency,
                          BigDecimal expectedAmountKrw, LocalDate krwConvertedDate, BigDecimal krwExchangeRate) {
        this.member = member;
        this.candidate = candidate;
        this.name = name;
        this.category = category;
        this.merchantAlias = merchantAlias;
        this.billerMerchant = billerMerchant;
        this.paymentMethod = paymentMethod;
        this.expectedPayDay = expectedPayDay;
        this.expectedAmount = expectedAmount;
        this.expectedCurrency = expectedCurrency;
        this.expectedAmountKrw = expectedAmountKrw;
        this.krwConvertedDate = krwConvertedDate;
        this.krwExchangeRate = krwExchangeRate;
        this.status = FixedExpenseStatus.ACTIVE;
    }

    /** 경로 A: 탐지 후보 승인. 카드 거래에서 탐지됐으므로 결제수단은 항상 CARD로 고정한다. */
    public static FixedExpense fromCandidate(Member member, FixedExpenseCandidate candidate, String name,
                                              Category category, MerchantAlias merchantAlias, Merchant billerMerchant,
                                              Short expectedPayDay, BigDecimal expectedAmount, String expectedCurrency,
                                              BigDecimal expectedAmountKrw, LocalDate krwConvertedDate,
                                              BigDecimal krwExchangeRate) {
        return new FixedExpense(member, candidate, name, category, merchantAlias, billerMerchant,
                FixedExpensePaymentMethod.CARD, expectedPayDay, expectedAmount, expectedCurrency, expectedAmountKrw,
                krwConvertedDate, krwExchangeRate);
    }

    /** 경로 B: 직접 등록. TRANSFER면 merchantAlias·billerMerchant가 NULL일 수 있다. */
    public static FixedExpense registerDirect(Member member, String name, Category category,
                                               MerchantAlias merchantAlias, FixedExpensePaymentMethod paymentMethod,
                                               Short expectedPayDay, BigDecimal expectedAmount, String expectedCurrency,
                                               BigDecimal expectedAmountKrw, LocalDate krwConvertedDate,
                                               BigDecimal krwExchangeRate) {
        return new FixedExpense(member, null, name, category, merchantAlias, null, paymentMethod, expectedPayDay,
                expectedAmount, expectedCurrency, expectedAmountKrw, krwConvertedDate, krwExchangeRate);
    }

    /** 금액·결제일 수정(F-04). */
    public void updateExpected(BigDecimal expectedAmount, String expectedCurrency, BigDecimal expectedAmountKrw,
                                LocalDate krwConvertedDate, BigDecimal krwExchangeRate, Short expectedPayDay) {
        this.expectedAmount = expectedAmount;
        this.expectedCurrency = expectedCurrency;
        this.expectedAmountKrw = expectedAmountKrw;
        this.krwConvertedDate = krwConvertedDate;
        this.krwExchangeRate = krwExchangeRate;
        this.expectedPayDay = expectedPayDay;
    }

    /**
     * 주기 시작(budget 생성) 시 해외 구독의 원화 스냅샷만 최신 환율로 갱신한다(dailyBudget.md §1, erd.md §5 환산 시점 ②).
     * 매칭 기준인 {@code expectedAmount}·{@code expectedCurrency}는 건드리지 않는다 — 예산 차감값만 바뀐다.
     */
    public void refreshKrwSnapshot(BigDecimal expectedAmountKrw, LocalDate krwConvertedDate, BigDecimal krwExchangeRate) {
        this.expectedAmountKrw = expectedAmountKrw;
        this.krwConvertedDate = krwConvertedDate;
        this.krwExchangeRate = krwExchangeRate;
    }

    /** 해지. 물리 삭제하지 않는다 — 지난달까지의 이행 기록은 그대로 남는다. */
    public void cancel() {
        this.status = FixedExpenseStatus.CANCELED;
    }

    /**
     * 직접 등록(경로 B) 당시엔 결제내역이 없어 알 수 없었던 biller를 F-05 미납 확인 시점에 소급 채운다
     * (troubleshooting/billerProblem.md). 이미 채워진 값은 덮어쓰지 않는다 — 최초 확인이 정본이다.
     */
    public void assignBillerMerchant(Merchant billerMerchant) {
        if (this.billerMerchant == null) {
            this.billerMerchant = billerMerchant;
        }
    }

    public boolean isActive() {
        return status == FixedExpenseStatus.ACTIVE;
    }
}
