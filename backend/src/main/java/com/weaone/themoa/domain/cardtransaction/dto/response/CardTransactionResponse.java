package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CardTransactionResponse(
        Long id,
        String source,
        String paymentMethod,
        LocalDate usedDate,
        LocalDateTime usedAt,
        BigDecimal amount,
        BigDecimal netAmount,
        String currencyCode,
        BigDecimal originalAmount,
        boolean amountUserCorrected,
        String status,
        Long categoryId,
        String categoryName,
        boolean categoryUserCorrected,
        String merchantNameRaw,
        String merchantDisplayName,
        boolean hasPersonalMerchantLabel,
        Long merchantAliasId,
        BigDecimal canceledAmount,
        boolean cancelAmountUncertain,
        boolean cancelAmountUserCorrected,
        String cardOrganizationName,
        String cardNumberMasked,
        Short installmentMonths,
        String memo,
        Long fixedExpenseId,
        String fixedExpenseName
) {

    public static CardTransactionResponse from(CardTransaction transaction) {
        return from(transaction, null);
    }

    /** {@code personalLabel}은 이 거래를 보는 회원이 이 서비스(merchantAlias)에 붙인 개인 표시명이다 — 있으면 항상 최우선이다. */
    public static CardTransactionResponse from(CardTransaction transaction, String personalLabel) {
        return new CardTransactionResponse(
                transaction.getId(),
                transaction.getSource().name(),
                transaction.getPaymentMethod().name(),
                transaction.getUsedDate(),
                transaction.getUsedAt(),
                transaction.getAmount(),
                transaction.getNetAmount(),
                transaction.getCurrencyCode(),
                transaction.getOriginalAmount(),
                transaction.isAmountUserCorrected(),
                transaction.getStatus().name(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.isCategoryUserCorrected(),
                transaction.getMerchantNameRaw(),
                resolveDisplayName(transaction, personalLabel),
                personalLabel != null,
                transaction.getMerchantAlias() != null ? transaction.getMerchantAlias().getId() : null,
                transaction.getCanceledAmount(),
                transaction.isCancelAmountUncertain(),
                transaction.isCancelAmountUserCorrected(),
                transaction.getCard() != null ? transaction.getCard().getCardConnection().getCardIssuer().getName() : null,
                transaction.getCard() != null ? transaction.getCard().getCardNumberMasked() : null,
                transaction.getInstallmentMonths(),
                transaction.getMemo(),
                transaction.getFixedExpense() != null ? transaction.getFixedExpense().getId() : null,
                transaction.getFixedExpense() != null ? transaction.getFixedExpense().getName() : null
        );
    }

    /** 화면 표시 폴백: 회원 개인 표시명 → canonical_service_name → merchant.display_name → 원본(merchant.md §6). */
    private static String resolveDisplayName(CardTransaction transaction, String personalLabel) {
        if (personalLabel != null) {
            return personalLabel;
        }
        if (transaction.getMerchantAlias() != null) {
            return transaction.getMerchantAlias().getCanonicalServiceName();
        }
        if (transaction.getMerchant() != null && transaction.getMerchant().getDisplayName() != null) {
            return transaction.getMerchant().getDisplayName();
        }
        return transaction.getMerchantNameRaw();
    }
}
