package com.weaone.themoa.domain.cardtransaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CategoryTrendPointResponse(
        Long budgetId,
        String yearMonth,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        LocalDate dataEndDate,
        int comparedDayCount,
        BigDecimal amount
) {
}
