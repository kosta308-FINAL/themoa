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
                transaction.getCanceledAmount(),
                transaction.isCancelAmountUncertain(),
                transaction.getMemo()
        );
    }
}
