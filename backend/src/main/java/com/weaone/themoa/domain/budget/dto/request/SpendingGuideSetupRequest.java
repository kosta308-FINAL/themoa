package com.weaone.themoa.domain.budget.dto.request;

import com.weaone.themoa.domain.member.entity.IncomeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * S-00A 소비 가이드 최초 설정(MOA-S-BUD-BGT-01). incomeType=SALARY면 salaryAmount가,
 * HOURLY면 hourlyWage·workSchedule(최소 1건)이 필요하다 — 소득유형별 필수값은 record
 * 애노테이션이 아니라 {@code SpendingGuideService.setup()}에서 검증한다(조건부 필수라 @NotNull로
 * 표현할 수 없음). 급여일은 소득유형과 무관하게 1~31 공통.
 */
public record SpendingGuideSetupRequest(
        @NotNull IncomeType incomeType,
        BigDecimal salaryAmount,
        BigDecimal hourlyWage,
        @Valid List<WorkScheduleItem> workSchedule,
        @NotNull @Min(1) @Max(31) Integer payday) {
}
