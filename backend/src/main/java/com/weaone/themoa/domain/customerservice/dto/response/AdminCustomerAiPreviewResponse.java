package com.weaone.themoa.domain.customerservice.dto.response;

public record AdminCustomerAiPreviewResponse(
        AdminCustomerAiSearchResponse search,
        String answerMarkdown,
        boolean needsHumanSupport
) {
}
