package com.weaone.themoa.domain.customerservice.rag;

public record CustomerKnowledgeSearchResult(
        CustomerKnowledgeDocument document,
        Double score
) {
}
