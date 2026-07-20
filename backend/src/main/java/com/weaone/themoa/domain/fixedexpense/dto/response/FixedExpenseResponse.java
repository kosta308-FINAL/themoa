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
        String status,
        String paymentStatus
) {

    /**
     * @param paymentStatus 이번 주기 이행 상태 배지(view/fixedExpense.md §4) — {@code PAID}/{@code DUE_SOON}/
     *                       {@code MISSED} 중 하나거나, 카드 미연동·이체형이면 null(배지 없음).
     *                       {@link com.weaone.themoa.domain.fixedexpense.service.FixedExpensePaymentStatusService}가 계산한다.
     */
    public static FixedExpenseResponse from(FixedExpense fixedExpense, String paymentStatus) {
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
                fixedExpense.getStatus().name(),
                paymentStatus
        );
    }
}
