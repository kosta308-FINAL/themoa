package com.weaone.themoa.domain.customerservice.dto.request;

public record AdminCustomerAiPreviewRequest(
        String query,
        Integer topK,
        Double minimumSimilarity,
        String systemPrompt
) {
}
