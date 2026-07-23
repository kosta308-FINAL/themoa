package com.weaone.themoa.domain.calendar.dto.response;

import com.weaone.themoa.domain.calendar.entity.CalendarEventType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalendarEventResponse(
        String eventKey,
        CalendarEventType eventType,
        LocalDate eventDate,
        String title,
        BigDecimal amount,
        Long sourceId,
        boolean editable
) {
}
