package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.CodefValueType;
import com.weaone.themoa.domain.cardconnection.repository.CardRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalRecord;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.service.CategoryClassificationService;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseMatchingService;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityResult;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 카드 거래 수집 파이프라인의 단일 진입점(cardtransaction.md §1): ①중복 판정 ②해외 원화 환산 ③가맹점 신원
 * ④카테고리 분류 ⑤고정지출 매칭(fixedExpense.md §5)을 거쳐 저장한다. 이미 저장된 행(재수집)이면
 * §6-3 갱신 규칙(사용자 정정 보호)만 적용하고 카테고리는 건드리지 않지만, ⑤는 예외다 — 아직 어떤
 * 고정지출에도 안 태깅된 행은 그 사이 새로 등록된 규칙이 있을 수 있어 재수집 때도 매칭을 다시 시도한다
 * (이중차감 방지의 근간, §6-3). 이미 태깅된 행은 재매칭하지 않는다(태그를 흔들 위험만 있다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionCollectionService {

    private static final DateTimeFormatter USED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter USED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_USED_TIME = "000000";
    private static final String CURRENCY_KRW = "KRW";

    private final CardRepository cardRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final MerchantIdentityService merchantIdentityService;
    private final MerchantRepository merchantRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final CategoryClassificationService categoryClassificationService;
    private final ExchangeRateService exchangeRateService;
    private final FixedExpenseMatchingService fixedExpenseMatchingService;

    @Transactional
    public CollectionOutcome collect(Member member, CardConnection cardConnection, CardIssuer cardIssuer,
                                      CodefApprovalRecord record) {
        Card card = findOrCreateCard(member, cardConnection, record);
        LocalDate usedDate = LocalDate.parse(record.resUsedDate(), USED_DATE_FORMAT);
        LocalDateTime usedAt = parseUsedAt(record);
        String approvalNo = record.resApprovalNo();
        boolean type2CancellationRow = isType2CancellationRow(record);
        CancellationInfo cancellation = resolveCancellation(record, cardIssuer, type2CancellationRow);

        Optional<CardTransaction> existing = cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(
                        member.getId(), card.getId(), usedDate, usedAt, approvalNo);
        if (existing.isPresent()) {
            reconcile(existing.get(), member, cardIssuer, record, usedDate, cancellation, type2CancellationRow);
            return CollectionOutcome.UPDATED;
        }
        return create(member, card, cardIssuer, record, usedDate, usedAt, approvalNo, cancellation, type2CancellationRow);
    }

    private CollectionOutcome create(Member member, Card card, CardIssuer cardIssuer, CodefApprovalRecord record,
                                      LocalDate usedDate, LocalDateTime usedAt, String approvalNo,
                                      CancellationInfo cancellation, boolean type2CancellationRow) {
        FxAmount fx;
        try {
            fx = resolveFx(record, cardIssuer, usedDate);
        } catch (ExchangeRateUnavailableException e) {
            log.warn("환율 정보를 구하지 못해 거래 수집을 건너뜁니다. approvalNo={}", approvalNo);
            return CollectionOutcome.SKIPPED;
        }

        String merchantNameRaw = record.resMemberStoreName();
        String merchantTypeRaw = blankToNull(record.resMemberStoreType());
        MerchantIdentityResult identity = merchantIdentityService.resolve(member.getId(), merchantNameRaw);
        Category category = categoryClassificationService.classify(merchantNameRaw, merchantTypeRaw);
        CancellationInfo finalCancellation = finalizeCancellation(cancellation, fx.amount(), type2CancellationRow);

        CardTransaction transaction = CardTransaction.sync(member, card, category, approvalNo, usedDate, usedAt,
                fx.amount(), fx.originalAmount(), fx.currencyCode(), fx.exchangeRate(), fx.exchangeRateEstimated(),
                finalCancellation.status(), finalCancellation.canceledAmount(), finalCancellation.cancelAmountUncertain(),
                merchantNameRaw, merchantTypeRaw, blankToNull(record.resMemberStoreCorpNo()),
                blankToNull(record.resMemberStoreAddr()),
                parseInstallmentMonths(record.resInstallmentMonth()));
        transaction.assignMerchant(merchantReferenceOrNull(identity.merchantId()),
                merchantAliasReferenceOrNull(identity.merchantAliasId()));

        try {
            cardTransactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            // 동시 재수집 경합: 다른 스레드가 먼저 같은 UNIQUE 키로 저장했다. 새로 만든 행이 아니라 재수집으로 취급한다.
            cardTransactionRepository
                    .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(
                            member.getId(), card.getId(), usedDate, usedAt, approvalNo)
                    .ifPresentOrElse(
                            found -> reconcile(found, member, cardIssuer, record, usedDate, cancellation, type2CancellationRow),
                            () -> { throw e; });
            return CollectionOutcome.UPDATED;
        }
        fixedExpenseMatchingService.match(transaction);
        return CollectionOutcome.CREATED;
    }

    private void reconcile(CardTransaction transaction, Member member, CardIssuer cardIssuer,
                            CodefApprovalRecord record, LocalDate usedDate, CancellationInfo cancellation,
                            boolean type2CancellationRow) {
        String merchantNameRaw = transaction.getMerchantNameRaw();
        MerchantIdentityResult identity = merchantIdentityService.resolve(member.getId(), merchantNameRaw);

        BigDecimal amount = transaction.getAmount();
        BigDecimal exchangeRate = transaction.getExchangeRate();
        boolean exchangeRateEstimated = transaction.isExchangeRateEstimated();
        // 사용자가 이미 정정한 금액은 재계산 자체를 시도하지 않는다 — 어차피 엔티티가 덮어쓰지 않는다(§4).
        if (!transaction.isAmountUserCorrected()) {
            try {
                FxAmount fx = resolveFx(record, cardIssuer, usedDate);
                amount = fx.amount();
                exchangeRate = fx.exchangeRate();
                exchangeRateEstimated = fx.exchangeRateEstimated();
            } catch (ExchangeRateUnavailableException e) {
                log.warn("재수집 중 환율 재계산에 실패해 기존 금액을 유지합니다. approvalNo={}", record.resApprovalNo());
            }
        }
        CancellationInfo finalCancellation = finalizeCancellation(cancellation, amount, type2CancellationRow);

        transaction.reconcileOnResync(finalCancellation.status(), finalCancellation.canceledAmount(),
                finalCancellation.cancelAmountUncertain(), amount, exchangeRate, exchangeRateEstimated,
                merchantReferenceOrNull(identity.merchantId()), merchantAliasReferenceOrNull(identity.merchantAliasId()));

        if (finalCancellation.status() == TransactionStatus.CANCELED) {
            fixedExpenseMatchingService.unmatchIfCanceled(transaction);
        } else if (transaction.getFixedExpense() == null) {
            // 이 거래가 저장된 이후 새로 등록된 fixed_expense 규칙이 있을 수 있다 — 소급 매칭 시도(§6-3).
            fixedExpenseMatchingService.match(transaction);
        }
    }

    /**
     * 전체취소는 amount 전액이 취소금액이다(§3-3) — FX 환산 후 금액이 확정된 뒤에만 채울 수 있다.
     * Type 2 별도 음수 취소행(§3-5)은 예외다: amount 자체가 이미 음수라 canceled_amount를 채우면
     * getNetAmount()가 이중 차감되므로, status가 CANCELED여도 canceled_amount는 계속 비운다.
     */
    private CancellationInfo finalizeCancellation(CancellationInfo cancellation, BigDecimal amount,
                                                   boolean type2CancellationRow) {
        if (!type2CancellationRow && cancellation.status() == TransactionStatus.CANCELED) {
            return new CancellationInfo(cancellation.status(), amount, false);
        }
        return cancellation;
    }

    /** Type 2 취소행 판별(§3-5): 환산 전 resUsedAmount < 0이면 카드사가 별도 행으로 준 취소행이다. */
    private boolean isType2CancellationRow(CodefApprovalRecord record) {
        return parseAmount(record.resUsedAmount()).signum() < 0;
    }

    private Card findOrCreateCard(Member member, CardConnection cardConnection, CodefApprovalRecord record) {
        String cardNumberMasked = record.resCardNo();
        String cardName = StringUtils.hasText(record.resCardName()) ? record.resCardName() : cardNumberMasked;
        return cardRepository.findByCardConnection_IdAndCardNumberMasked(cardConnection.getId(), cardNumberMasked)
                .orElseGet(() -> {
                    try {
                        return cardRepository.save(Card.observe(member, cardConnection, cardName, cardNumberMasked));
                    } catch (DataIntegrityViolationException e) {
                        return cardRepository
                                .findByCardConnection_IdAndCardNumberMasked(cardConnection.getId(), cardNumberMasked)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private LocalDateTime parseUsedAt(CodefApprovalRecord record) {
        String time = StringUtils.hasText(record.resUsedTime()) ? record.resUsedTime() : DEFAULT_USED_TIME;
        return LocalDateTime.parse(record.resUsedDate() + time, USED_AT_FORMAT);
    }

    /** resCancelYN → status 매핑(cardtransaction.md §3-1) + 취소금액 산정(§3-3, §3-4). */
    private CancellationInfo resolveCancellation(CodefApprovalRecord record, CardIssuer cardIssuer,
                                                  boolean type2CancellationRow) {
        TransactionStatus status = switch (record.resCancelYN()) {
            case "0" -> TransactionStatus.APPROVED;
            case "1" -> TransactionStatus.CANCELED;
            case "2" -> TransactionStatus.PARTIAL_CANCELED;
            case "3" -> TransactionStatus.REJECTED;
            default -> throw new IllegalStateException("알 수 없는 resCancelYN 값입니다: " + record.resCancelYN());
        };

        if (type2CancellationRow) {
            // Type 2 별도 취소행(§3-5): canceled_amount는 채우지 않는다 — amount 자체가 음수라
            // getNetAmount()가 그 음수를 그대로 반환해야 한다(§3-3의 원 거래 취소금액 산정과 무관).
            return new CancellationInfo(status, null, false);
        }

        return switch (status) {
            case APPROVED, REJECTED -> new CancellationInfo(status, null, false);
            // 전체취소는 실지출 0이 목표라, FX 환산 후의 amount를 그대로 취소금액으로 맞춘다(§3-3).
            case CANCELED -> new CancellationInfo(status, null, false);
            case PARTIAL_CANCELED -> isCancelAmountUncertain(record, cardIssuer)
                    ? new CancellationInfo(status, null, true)
                    : new CancellationInfo(status, parseAmount(record.resCancelAmount()), false);
        };
    }

    /**
     * 삼성·신한은 통화 무관하게 항상 불확실하고, 롯데는 해외 부분취소 건에만 해당한다(§3-4).
     * 감지 기준은 FX-01과 동일하게 통화코드로 통일한다.
     */
    private boolean isCancelAmountUncertain(CodefApprovalRecord record, CardIssuer cardIssuer) {
        if (!cardIssuer.isCancelAmountUncertain()) {
            return false;
        }
        if (!cardIssuer.isCancelAmountUncertainForeignOnly()) {
            return true;
        }
        String currency = StringUtils.hasText(record.resAccountCurrency()) ? record.resAccountCurrency() : CURRENCY_KRW;
        return !CURRENCY_KRW.equals(currency);
    }

    /** 해외결제 원화 환산(FX-00~03, cardtransaction.md §4). */
    private FxAmount resolveFx(CodefApprovalRecord record, CardIssuer cardIssuer, LocalDate usedDate) {
        BigDecimal usedAmount = parseAmount(record.resUsedAmount());
        String currency = StringUtils.hasText(record.resAccountCurrency()) ? record.resAccountCurrency() : CURRENCY_KRW;

        if (cardIssuer.getFxType() == CodefValueType.TYPE2) {
            // type2 카드: resUsedAmount가 이미 원화. 외화 원금이 따로 오지 않는다(§4 FX-00).
            return new FxAmount(usedAmount, null, currency, null, false);
        }

        if (CURRENCY_KRW.equals(currency)) {
            return new FxAmount(usedAmount, null, CURRENCY_KRW, null, false);
        }

        BigDecimal originalAmount = usedAmount;
        String krwAmtRaw = record.resKRWAmt();
        if (StringUtils.hasText(krwAmtRaw)) {
            BigDecimal krwAmount = parseAmount(krwAmtRaw);
            BigDecimal rate = krwAmount.divide(originalAmount, 4, RoundingMode.HALF_UP);
            return new FxAmount(krwAmount, originalAmount, currency, rate, false);
        }

        ExchangeRateResult rateResult = exchangeRateService.getRate(currency, usedDate);
        BigDecimal krwAmount = originalAmount.multiply(rateResult.rate()).setScale(2, RoundingMode.HALF_UP);
        return new FxAmount(krwAmount, originalAmount, currency, rateResult.rate(), rateResult.estimated());
    }

    private Merchant merchantReferenceOrNull(Long merchantId) {
        return merchantId == null ? null : merchantRepository.getReferenceById(merchantId);
    }

    private MerchantAlias merchantAliasReferenceOrNull(Long merchantAliasId) {
        return merchantAliasId == null ? null : merchantAliasRepository.getReferenceById(merchantAliasId);
    }

    private BigDecimal parseAmount(String raw) {
        return new BigDecimal(raw.trim());
    }

    private Short parseInstallmentMonths(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return Short.parseShort(raw.trim());
    }

    private String blankToNull(String raw) {
        return StringUtils.hasText(raw) ? raw : null;
    }
}
