package com.weaone.themoa.domain.budget.dto.request;

import com.weaone.themoa.domain.budget.service.BudgetApplyScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** 월급 수정(MOA-S-BUD-BGT-08). applyFrom으로 이번/다음 주기 적용을 가른다. */
public record SalaryUpdateRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull BudgetApplyScope applyFrom) {
}
