package com.weaone.themoa.domain.calendar.dto.response;

import com.weaone.themoa.domain.calendar.entity.CalendarSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalendarScheduleResponse(
        Long id,
        String title,
        LocalDate scheduleDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CalendarScheduleResponse from(CalendarSchedule schedule) {
        return new CalendarScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getScheduleDate(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
