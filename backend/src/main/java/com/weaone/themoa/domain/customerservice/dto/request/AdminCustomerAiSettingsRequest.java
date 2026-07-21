package com.weaone.themoa.domain.customerservice.dto.request;

public record AdminCustomerAiSettingsRequest(
        Integer topK,
        Double minimumSimilarity,
        String systemPrompt
) {
}
