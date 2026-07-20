package com.weaone.themoa.domain.budget.dto.request;

import com.weaone.themoa.domain.budget.service.BudgetApplyScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/** 시급제(HOURLY) 시급·근무스케줄 수정. 적용 시점 규칙은 월급 수정(MOA-S-BUD-BGT-08)과 같다. */
public record WorkScheduleUpdateRequest(
        @NotNull @Positive BigDecimal hourlyWage,
        @NotEmpty @Valid List<WorkScheduleItem> workSchedule,
        @NotNull BudgetApplyScope applyFrom) {
}
