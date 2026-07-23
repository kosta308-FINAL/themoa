package com.weaone.themoa.domain.calendar.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CalendarScheduleUpdateRequest(
        @NotBlank
        String title,

        @NotNull
        LocalDate scheduleDate
) {
}
