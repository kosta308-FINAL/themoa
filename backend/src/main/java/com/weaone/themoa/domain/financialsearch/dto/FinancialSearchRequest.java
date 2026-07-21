package com.weaone.themoa.domain.financialsearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 검색 API 요청 바디. sort를 안 보내면 관련도순(RELEVANCE)으로 기본 처리.
public record FinancialSearchRequest(
        @NotBlank
        @Size(min = 2, max = 200)
        String query,
        FinancialSortOption sort
) {
    public FinancialSearchRequest {
        if (sort == null) {
            sort = FinancialSortOption.RELEVANCE;
        }
    }
}
