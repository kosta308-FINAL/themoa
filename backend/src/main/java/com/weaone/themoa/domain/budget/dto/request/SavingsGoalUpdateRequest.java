package com.weaone.themoa.domain.budget.dto.request;

import com.weaone.themoa.domain.budget.service.BudgetApplyScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** 저축 목표 설정·수정(MOA-S-BUD-BGT-03). 0원 허용(미설정 해제 개념). */
public record SavingsGoalUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotNull BudgetApplyScope applyFrom) {
}
