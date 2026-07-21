package com.weaone.themoa.domain.customerservice.dto.request;

public record AdminCustomerAiSearchRequest(
        String query,
        Integer topK,
        Double minimumSimilarity
) {
}
