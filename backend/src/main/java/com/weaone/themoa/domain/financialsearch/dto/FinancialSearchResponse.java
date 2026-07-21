package com.weaone.themoa.domain.financialsearch.dto;

import java.util.List;

public record FinancialSearchResponse(
        String query,
        String sort,
        int resultCount,
        List<FinancialSearchResultItem> results,
        String message,
        // 검색 결과가 완전히 없을 때만 채워지는, AI가 제안하는 대안 검색어 목록.
        List<String> suggestedQueries
) {
}
