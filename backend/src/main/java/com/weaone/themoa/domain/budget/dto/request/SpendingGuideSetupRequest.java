package com.weaone.themoa.domain.budget.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** S-00A 소비 가이드 최초 설정(MOA-S-BUD-BGT-01). 월급은 양수, 급여일은 1~31. */
public record SpendingGuideSetupRequest(
        @NotNull @Positive BigDecimal salaryAmount,
        @NotNull @Min(1) @Max(31) Integer payday) {
}
