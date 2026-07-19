package com.weaone.themoa.domain.policy.admin.dto;

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
