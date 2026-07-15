package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 카테고리별 소비 비중/내역(category.md §6·§7). */
public record CategorySummaryListResponse(
        BigDecimal totalAmount,
        List<CategorySummaryResponse> items
) {

    public static CategorySummaryListResponse from(List<CardTransactionRepository.CategorySummary> summaries) {
        BigDecimal totalAmount = summaries.stream()
                .map(CardTransactionRepository.CategorySummary::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CategorySummaryResponse> items = summaries.stream()
                .map(summary -> new CategorySummaryResponse(
                        summary.getCategoryId(),
                        summary.getCategoryName(),
                        summary.getTotalAmount(),
                        summary.getTransactionCount(),
                        percentageOf(summary.getTotalAmount(), totalAmount)))
                .toList();
        return new CategorySummaryListResponse(totalAmount, items);
    }

    private static BigDecimal percentageOf(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
