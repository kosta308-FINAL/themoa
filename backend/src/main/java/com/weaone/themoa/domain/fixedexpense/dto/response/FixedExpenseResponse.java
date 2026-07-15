package com.weaone.themoa.domain.fixedexpense.dto.response;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;

import java.math.BigDecimal;

public record FixedExpenseResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        String merchantAliasName,
        String paymentMethod,
        Short expectedPayDay,
        BigDecimal expectedAmount,
        String expectedCurrency,
        BigDecimal expectedAmountKrw,
        String status
) {

    public static FixedExpenseResponse from(FixedExpense fixedExpense) {
        return new FixedExpenseResponse(
                fixedExpense.getId(),
                fixedExpense.getName(),
                fixedExpense.getCategory().getId(),
                fixedExpense.getCategory().getName(),
                fixedExpense.getMerchantAlias() == null ? null : fixedExpense.getMerchantAlias().getCanonicalServiceName(),
                fixedExpense.getPaymentMethod().name(),
                fixedExpense.getExpectedPayDay(),
                fixedExpense.getExpectedAmount(),
                fixedExpense.getExpectedCurrency(),
                fixedExpense.getExpectedAmountKrw(),
                fixedExpense.getStatus().name()
        );
    }
}
