package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CardTransactionResponse(
        Long id,
        LocalDate usedDate,
        LocalDateTime usedAt,
        BigDecimal amount,
        BigDecimal netAmount,
        String currencyCode,
        BigDecimal originalAmount,
        String status,
        Long categoryId,
        String categoryName,
        boolean categoryUserCorrected,
        String merchantNameRaw,
        String merchantDisplayName,
        BigDecimal canceledAmount,
        boolean cancelAmountUncertain,
        String memo
) {

    public static CardTransactionResponse from(CardTransaction transaction) {
        return new CardTransactionResponse(
                transaction.getId(),
                transaction.getUsedDate(),
                transaction.getUsedAt(),
                transaction.getAmount(),
                transaction.getNetAmount(),
                transaction.getCurrencyCode(),
                transaction.getOriginalAmount(),
                transaction.getStatus().name(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.isCategoryUserCorrected(),
                transaction.getMerchantNameRaw(),
                resolveDisplayName(transaction),
                transaction.getCanceledAmount(),
                transaction.isCancelAmountUncertain(),
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
