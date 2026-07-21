package com.weaone.themoa.domain.customerservice.rag;

public record CustomerKnowledgeDocument(
        String id,
        CustomerKnowledgeSourceType sourceType,
        String sourceId,
        String category,
        String title,
        String content
) {
}
