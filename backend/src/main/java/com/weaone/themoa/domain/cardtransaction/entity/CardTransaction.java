package com.weaone.themoa.domain.cardtransaction.entity;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 거래내역(erd.md "카드 연동" § 거래내역). 이번 구현 범위는 카드 수집(source=SYNC)뿐이다 — 수기 입력(MANUAL)
 * 생성 로직과 대체(replaced_at/replaced_by, entryMode.md 백필)는 별도 기능이라 이 엔티티에 아직 두지 않는다.
 */
@Entity
@Table(name = "card_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_card_transaction_dedup",
                columnNames = {"member_id", "card_id", "used_date", "used_at", "approval_no"}))
@Check(constraints = "source <> 'SYNC' OR (card_id IS NOT NULL AND approval_no IS NOT NULL)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    /** 전역 시드 매칭 결과만 담는 {@link Merchant#getMerchantAlias()}와 달리, 여기는 이 회원의 해석 결과다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** 이중차감 방지 태그(fixedExpense.md §5). NULL이면 "오늘 쓴 돈"에 포함되는 일반 소비다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_expense_id")
    private FixedExpense fixedExpense;

    /** true면 ④자동분류·⑤고정지출 카테고리 대입·관리자 소급 재분류 전 경로가 category_id를 건드리지 않는다. */
    @Column(name = "category_user_corrected", nullable = false)
    private boolean categoryUserCorrected;

    @Column(name = "approval_no", length = 50)
    private String approvalNo;

    @Column(name = "used_date", nullable = false)
    private LocalDate usedDate;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** 외화 원금(예: USD 22.00). type2 카드(fx_type)는 이용금액이 이미 원화라 원금이 없어 NULL이다. */
    @Column(name = "original_amount", precision = 14, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "exchange_rate_estimated", nullable = false)
    private boolean exchangeRateEstimated;

    @Column(name = "amount_user_corrected", nullable = false)
    private boolean amountUserCorrected;

    @Column(name = "canceled_amount", precision = 14, scale = 2)
    private BigDecimal canceledAmount;

    @Column(name = "cancel_amount_uncertain", nullable = false)
    private boolean cancelAmountUncertain;

    @Column(name = "cancel_amount_user_corrected", nullable = false)
    private boolean cancelAmountUserCorrected;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 10)
    private PaymentMethod paymentMethod;

    @Column(name = "merchant_name_raw", nullable = false, length = 255)
    private String merchantNameRaw;

    @Column(name = "merchant_type_raw", length = 100)
    private String merchantTypeRaw;

    /**
     * 가맹점 사업자번호 원본(resMemberStoreCorpNo). 신원 판별·dedup 키로는 쓰지 않는다 — PG 오염으로
     * 한 번호에 무관 가맹점이 여럿 묶일 수 있다(merchant.md §1). 원본 보관 목적으로만 둔다.
     */
    @Column(name = "merchant_corp_no_raw", length = 50)
    private String merchantCorpNoRaw;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "installment_months")
    private Short installmentMonths;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    private CardTransaction(Member member, Card card, Category category, String approvalNo,
                             LocalDate usedDate, LocalDateTime usedAt, BigDecimal amount,
                             BigDecimal originalAmount, String currencyCode, BigDecimal exchangeRate,
                             boolean exchangeRateEstimated, TransactionStatus status,
                             BigDecimal canceledAmount, boolean cancelAmountUncertain,
                             String merchantNameRaw, String merchantTypeRaw, String merchantCorpNoRaw,
                             String address, Short installmentMonths) {
        this.member = member;
        this.card = card;
        this.category = category;
        this.approvalNo = approvalNo;
        this.usedDate = usedDate;
        this.usedAt = usedAt;
        this.amount = amount;
        this.originalAmount = originalAmount;
        this.currencyCode = currencyCode;
        this.exchangeRate = exchangeRate;
        this.exchangeRateEstimated = exchangeRateEstimated;
        this.status = status;
        this.canceledAmount = canceledAmount;
        this.cancelAmountUncertain = cancelAmountUncertain;
        this.source = TransactionSource.SYNC;
        this.paymentMethod = PaymentMethod.CARD;
        this.merchantNameRaw = merchantNameRaw;
        this.merchantTypeRaw = merchantTypeRaw;
        this.merchantCorpNoRaw = merchantCorpNoRaw;
        this.address = address;
        this.installmentMonths = installmentMonths;
    }

    /** 카드 수집 신규 거래 저장(cardtransaction.md §1 파이프라인 결과). */
    public static CardTransaction sync(Member member, Card card, Category category, String approvalNo,
                                        LocalDate usedDate, LocalDateTime usedAt, BigDecimal amount,
                                        BigDecimal originalAmount, String currencyCode, BigDecimal exchangeRate,
                                        boolean exchangeRateEstimated, TransactionStatus status,
                                        BigDecimal canceledAmount, boolean cancelAmountUncertain,
                                        String merchantNameRaw, String merchantTypeRaw, String merchantCorpNoRaw,
                                        String address, Short installmentMonths) {
        return new CardTransaction(member, card, category, approvalNo, usedDate, usedAt, amount, originalAmount,
                currencyCode, exchangeRate, exchangeRateEstimated, status, canceledAmount, cancelAmountUncertain,
                merchantNameRaw, merchantTypeRaw, merchantCorpNoRaw, address, installmentMonths);
    }

    public void assignMerchant(Merchant merchant, MerchantAlias merchantAlias) {
        this.merchant = merchant;
        this.merchantAlias = merchantAlias;
    }

    /**
     * 수집 매칭 성공(또는 취소 시 해제) 태깅(fixedExpense.md §5). 매칭 성공 시 그 고정지출의 카테고리를
     * 대입한다 — 단 사용자가 건별로 이미 고친 거래는 덮어쓰지 않는다(merchant.md §5-D-6 가드,
     * category.md §2-④와 동일 원칙). 해제(null)는 카테고리를 되돌리지 않는다.
     */
    public void assignFixedExpense(FixedExpense fixedExpense) {
        this.fixedExpense = fixedExpense;
        if (fixedExpense != null && !this.categoryUserCorrected) {
            this.category = fixedExpense.getCategory();
        }
    }

    /**
     * 재수집(§6-3) 시 이미 저장된 행에 적용하는 갱신. 카테고리는 대상이 아니다 — ④(키워드·업종 매핑)의
     * 입력값은 최초 수집 스냅샷이라 재실행해도 결과가 같고, 관리자 소급 재분류는 별도 배치가 담당한다.
     */
    public void reconcileOnResync(TransactionStatus status, BigDecimal canceledAmount, boolean cancelAmountUncertain,
                                   BigDecimal amount, BigDecimal exchangeRate, boolean exchangeRateEstimated,
                                   Merchant merchant, MerchantAlias merchantAlias) {
        this.status = status;
        if (!this.cancelAmountUserCorrected) {
            this.canceledAmount = canceledAmount;
            this.cancelAmountUncertain = cancelAmountUncertain;
        }
        if (!this.amountUserCorrected) {
            this.amount = amount;
            this.exchangeRate = exchangeRate;
            this.exchangeRateEstimated = exchangeRateEstimated;
        }
        this.merchant = merchant;
        this.merchantAlias = merchantAlias;
    }

    /** 사용자 건별 카테고리 수정(category.md §2-④). 이후 어떤 자동 경로도 category_id를 덮어쓰지 않는다. */
    public void correctCategory(Category category) {
        this.category = category;
        this.categoryUserCorrected = true;
    }

    /** 취소금액 정정(§3-4). 카드사가 부정확한 값만 준 건(cancel_amount_uncertain=true)에 한해 허용된다. */
    public void correctCanceledAmount(BigDecimal canceledAmount) {
        this.canceledAmount = canceledAmount;
        this.cancelAmountUncertain = false;
        this.cancelAmountUserCorrected = true;
    }

    /** 외화 환산액 정정(§4). type1 카드의 환산 건(originalAmount != null)에 한해 허용된다. */
    public void correctAmount(BigDecimal amount) {
        this.amount = amount;
        this.amountUserCorrected = true;
    }

    /** 메모 자유 입력(`MOA-S-CAT-CTG-06`). 재수집이 덮어쓰지 않는다(cardtransaction.md §6-3). */
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    /** 실지출 = amount − COALESCE(canceled_amount, 0)(cardtransaction.md §3-3). */
    public BigDecimal getNetAmount() {
        return canceledAmount == null ? amount : amount.subtract(canceledAmount);
    }
}
