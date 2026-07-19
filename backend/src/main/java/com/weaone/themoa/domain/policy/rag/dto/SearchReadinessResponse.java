package com.weaone.themoa.domain.policy.rag.dto;

import java.util.List;

public record SearchReadinessResponse(
        boolean ready,
        long activePolicyCount,
        long projectionCount,
        long lexicalIndexDocumentCount,
        long syncedEmbeddingCount,
        List<String> missingSteps
) {
}
