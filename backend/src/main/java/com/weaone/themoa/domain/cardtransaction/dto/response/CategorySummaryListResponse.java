package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * S-01 카테고리 도넛(dayguide.md §3.4·§8.3). {@code positiveNetTotal}·{@code items}는 순액이 0원보다 큰
 * 실제 소비만 쓰고, {@code canceledTotal}은 이 주기 결제 중 취소된 금액을 별도로 안내한다.
 * {@code completedCycleResult}는 진행 중인 현재 주기이거나 데이터가 일부만 있는 주기({@code partialCycle=true})면
 * {@code null}이다.
 */
public record CategorySummaryListResponse(
        Long budgetId,
        String yearMonth,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        LocalDate dataStartDate,
        boolean partialCycle,
        boolean hasPrevious,
        boolean hasNext,
        Long previousBudgetId,
        Long nextBudgetId,
        BigDecimal positiveNetTotal,
        BigDecimal canceledTotal,
        List<CategorySummaryResponse> items,
        CompletedCycleResult completedCycleResult
) {

    public record CompletedCycleResult(
            BigDecimal budgetAmount,
            BigDecimal spentAmount,
            BigDecimal resultAmount,
            String resultType
    ) {
    }

    public static CategorySummaryListResponse of(Long budgetId, String yearMonth, LocalDate cycleStartDate,
            LocalDate cycleEndDate, LocalDate dataStartDate, boolean partialCycle, boolean hasPrevious,
            boolean hasNext, Long previousBudgetId, Long nextBudgetId,
            List<CardTransactionRepository.CategorySummary> summaries, BigDecimal canceledTotal,
            CompletedCycleResult completedCycleResult) {
        BigDecimal positiveNetTotal = summaries.stream()
                .map(CardTransactionRepository.CategorySummary::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CategorySummaryResponse> items = summaries.stream()
                .map(summary -> new CategorySummaryResponse(
                        summary.getCategoryId(),
                        summary.getCategoryName(),
                        summary.getTotalAmount(),
                        summary.getTransactionCount(),
                        percentageOf(summary.getTotalAmount(), positiveNetTotal)))
                .toList();
        return new CategorySummaryListResponse(budgetId, yearMonth, cycleStartDate, cycleEndDate, dataStartDate,
                partialCycle, hasPrevious, hasNext, previousBudgetId, nextBudgetId, positiveNetTotal, canceledTotal,
                items, completedCycleResult);
    }

    private static BigDecimal percentageOf(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
