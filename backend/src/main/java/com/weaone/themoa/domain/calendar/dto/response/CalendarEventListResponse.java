package com.weaone.themoa.domain.calendar.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CalendarEventListResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<CalendarEventResponse> items
) {
}
