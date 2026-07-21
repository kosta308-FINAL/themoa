package com.weaone.themoa.domain.customerservice.dto.response;

import java.util.List;

public record AdminCustomerAiSearchResponse(
        String query,
        int topK,
        double minimumSimilarity,
        int resultCount,
        List<AdminCustomerAiSearchResultResponse> results
) {
}
