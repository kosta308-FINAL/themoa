package com.weaone.themoa.domain.customerservice.dto.response;

import java.util.List;

public record AdminCustomerKnowledgeMetadataOptionsResponse(
        List<String> titles,
        List<String> categories
) {
}
