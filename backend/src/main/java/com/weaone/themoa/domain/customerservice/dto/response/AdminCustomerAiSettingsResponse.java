package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.rag.CustomerServiceRagSettingValues;

public record AdminCustomerAiSettingsResponse(
        int topK,
        double minimumSimilarity,
        String systemPrompt
) {
    public static AdminCustomerAiSettingsResponse from(CustomerServiceRagSettingValues values) {
        return new AdminCustomerAiSettingsResponse(
                values.topK(),
                values.minimumSimilarity(),
                values.systemPrompt());
    }
}
