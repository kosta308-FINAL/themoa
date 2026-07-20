package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.service.CycleStatus;

import java.time.LocalDate;

public record CategoryAnalysisCycleResponse(
        Long budgetId,
        String yearMonth,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        LocalDate dataEndDate,
        CycleStatus status,
        int comparedDayCount,
        Long previousBudgetId,
        Long nextBudgetId
) {
}
