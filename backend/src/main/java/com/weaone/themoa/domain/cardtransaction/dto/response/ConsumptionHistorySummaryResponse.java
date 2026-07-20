package com.weaone.themoa.domain.cardtransaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 전체 소비내역 상세 급여주기 요약(consumeHistoryDetail.md §3.1·§4). */
public record ConsumptionHistorySummaryResponse(
        CycleInfo cycle,
        BigDecimal cycleNetAmount,
        BigDecimal canceledAmount,
        ComparisonInfo comparison,
        List<MerchantTop5Item> merchantTop5,
        List<DailyTrendItem> dailyTrend
) {

    public record CycleInfo(
            Long budgetId,
            String yearMonth,
            LocalDate cycleStartDate,
            LocalDate cycleEndDate,
            LocalDate dataEndDate,
            String status,
            Long previousBudgetId,
            Long nextBudgetId
    ) {
    }

    public record ComparisonInfo(
            String basis,
            Long previousBudgetId,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            BigDecimal changeAmount,
            BigDecimal changeRate,
            String direction
    ) {
    }

    public record MerchantTop5Item(
            String merchantKey,
            String displayName,
            BigDecimal netAmount,
            long transactionCount
    ) {
    }

    public record DailyTrendItem(
            LocalDate date,
            BigDecimal netAmount
    ) {
    }
}
