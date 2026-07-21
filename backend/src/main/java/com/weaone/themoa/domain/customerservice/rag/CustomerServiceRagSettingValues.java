package com.weaone.themoa.domain.customerservice.rag;

public record CustomerServiceRagSettingValues(
        int topK,
        double minimumSimilarity,
        String systemPrompt
) {
}
