package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeSearchResult;

public record AdminCustomerAiSearchResultResponse(
        int rank,
        Double score,
        String sourceType,
        String sourceId,
        String category,
        String title,
        String content
) {
    public static AdminCustomerAiSearchResultResponse of(int rank, CustomerKnowledgeSearchResult result) {
        return new AdminCustomerAiSearchResultResponse(
                rank,
                result.score(),
                result.document().sourceType().name(),
                result.document().sourceId(),
                result.document().category(),
                result.document().title(),
                result.document().content());
    }
}
