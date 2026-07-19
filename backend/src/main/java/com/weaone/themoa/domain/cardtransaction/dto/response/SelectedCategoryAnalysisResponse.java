package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.service.CategoryHistoryStatus;

import java.math.BigDecimal;
import java.util.List;

public record SelectedCategoryAnalysisResponse(
        Long categoryId,
        String categoryName,
        BigDecimal selectedAmount,
        BigDecimal previousAmount,
        CategoryHistoryStatus historyStatus,
        List<CategoryTrendPointResponse> trend,
        CategoryPhaseResponse phase,
        CategoryDayTypeResponse dayType,
        List<CategoryInsightResponse> insights
) {
}
