package com.weaone.themoa.domain.fixedexpense.dto.response;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedExpensePaymentConfirmationResponse(
        Long transactionId,
        String merchantName,
        LocalDate usedDate,
        BigDecimal amount
) {

    public static FixedExpensePaymentConfirmationResponse from(FixedExpensePayment payment) {
        CardTransaction transaction = payment.getCardTransaction();
        String merchantName = transaction.getMerchantAlias() != null
                ? transaction.getMerchantAlias().getCanonicalServiceName()
                : transaction.getMerchant() != null && transaction.getMerchant().getDisplayName() != null
                ? transaction.getMerchant().getDisplayName()
                : transaction.getMerchantNameRaw();
        return new FixedExpensePaymentConfirmationResponse(
                transaction.getId(), merchantName, transaction.getUsedDate(), transaction.getNetAmount());
    }
}
