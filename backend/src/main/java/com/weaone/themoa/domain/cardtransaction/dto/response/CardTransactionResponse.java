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
        BigDecimal canceledAmount,
        boolean cancelAmountUncertain,
        boolean cancelAmountUserCorrected,
        String cardOrganizationName,
        String cardNumberMasked,
        Short installmentMonths,
        String memo
) {

    public static CardTransactionResponse from(CardTransaction transaction) {
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
                resolveDisplayName(transaction),
                transaction.getCanceledAmount(),
                transaction.isCancelAmountUncertain(),
                transaction.isCancelAmountUserCorrected(),
                transaction.getCard() != null ? transaction.getCard().getCardConnection().getCardIssuer().getName() : null,
                transaction.getCard() != null ? transaction.getCard().getCardNumberMasked() : null,
                transaction.getInstallmentMonths(),
                transaction.getMemo()
        );
    }

    /** 화면 표시 폴백: canonical_service_name → merchant.display_name → 원본(merchant.md §6). */
    private static String resolveDisplayName(CardTransaction transaction) {
        if (transaction.getMerchantAlias() != null) {
            return transaction.getMerchantAlias().getCanonicalServiceName();
        }
        if (transaction.getMerchant() != null && transaction.getMerchant().getDisplayName() != null) {
            return transaction.getMerchant().getDisplayName();
        }
        return transaction.getMerchantNameRaw();
    }
}
