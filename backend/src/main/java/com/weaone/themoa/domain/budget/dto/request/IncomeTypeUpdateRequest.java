package com.weaone.themoa.domain.budget.dto.request;

import com.weaone.themoa.domain.budget.service.BudgetApplyScope;
import com.weaone.themoa.domain.member.entity.IncomeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 소득유형 전환(HOURLY↔SALARY). incomeType=SALARY면 salaryAmount가, HOURLY면
 * hourlyWage·workSchedule(최소 1건)이 필요하다 — {@link SpendingGuideSetupRequest}와 같은 이유로
 * 소득유형별 필수값은 record 애노테이션이 아니라 서비스에서 검증한다. applyFrom 규칙은 월급 수정과 같다.
 */
public record IncomeTypeUpdateRequest(
        @NotNull IncomeType incomeType,
        BigDecimal salaryAmount,
        BigDecimal hourlyWage,
        @Valid List<WorkScheduleItem> workSchedule,
        @NotNull BudgetApplyScope applyFrom) {
}
