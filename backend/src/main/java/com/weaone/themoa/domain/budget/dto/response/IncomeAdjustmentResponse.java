package com.weaone.themoa.domain.budget.dto.response;

import com.weaone.themoa.domain.budget.entity.BudgetIncomeAdjustment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 이번 급여 주기의 수입 직접 입력 1건. */
public record IncomeAdjustmentResponse(
        Long id,
        BigDecimal amount,
        String memo,
        LocalDate occurredAt,
        LocalDateTime createdAt) {

    public static IncomeAdjustmentResponse from(BudgetIncomeAdjustment adjustment) {
        return new IncomeAdjustmentResponse(adjustment.getId(), adjustment.getAmount(), adjustment.getMemo(),
                adjustment.getOccurredAt(), adjustment.getCreatedAt());
    }
}
