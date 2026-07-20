package com.weaone.themoa.domain.budget.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/** 시급제(HOURLY) 요일별 반복 근무시간 1건. 최초 설정과 시급 수정 요청이 함께 쓴다. */
public record WorkScheduleItem(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull @Positive @DecimalMax("24.0") BigDecimal hours) {
}
