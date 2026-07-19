package com.weaone.themoa.domain.cardtransaction.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** 카테고리 소비 상세 화면(categoryDetail.md) 응답. */
public record CategoryAnalysisResponse(
        CategoryAnalysisCycleResponse cycle,
        String comparisonBasis,
        BigDecimal selectedCyclePositiveTotal,
        BigDecimal previousCyclePositiveTotal,
        List<CategoryComparisonResponse> categories,
        SelectedCategoryAnalysisResponse selectedCategory,
        String emptyReason
) {

    public static final String COMPARISON_BASIS_SAME_ELAPSED_DAYS = "SAME_ELAPSED_DAYS";
    public static final String EMPTY_REASON_NO_CATEGORY_SPEND = "NO_CATEGORY_SPEND";
}
