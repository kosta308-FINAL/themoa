package com.weaone.themoa.domain.fixedexpense.dto.request;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 경로 B: 직접 등록(F-03). {@code paymentMethod=CARD}면 {@code merchantAliasId}(기존 alias 선택) 또는
 * {@code newMerchantAliasName}(새로 만들기) 중 하나가 반드시 있어야 한다(fixedExpense.md §4).
 */
public record FixedExpenseDirectRegisterRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        @NotNull FixedExpensePaymentMethod paymentMethod,
        Long merchantAliasId,
        String newMerchantAliasName,
        @NotNull @Positive BigDecimal expectedAmount,
        String expectedCurrency,
        @NotNull @Min(1) @Max(31) Short expectedPayDay
) {
}
