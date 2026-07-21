package com.weaone.themoa.domain.customerservice.rag;

public record CustomerKnowledgeCitation(
        String title,
        String sourceType,
        String sourceId,
        String category,
        Double score
) {
}
