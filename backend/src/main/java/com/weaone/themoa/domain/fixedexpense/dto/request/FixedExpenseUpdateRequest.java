package com.weaone.themoa.domain.fixedexpense.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** 금액·결제일 수정(F-04). */
public record FixedExpenseUpdateRequest(
        @NotNull @Positive BigDecimal expectedAmount,
        String expectedCurrency,
        @NotNull @Min(1) @Max(31) Short expectedPayDay
) {
}
