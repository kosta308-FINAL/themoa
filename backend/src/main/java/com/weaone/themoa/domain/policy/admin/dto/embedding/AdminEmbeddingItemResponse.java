package com.weaone.themoa.domain.policy.admin.dto.embedding;

import java.time.LocalDateTime;

public record AdminEmbeddingItemResponse(
        Long embeddingSyncId,
        Integer policyId,
        String sourcePolicyId,
        String policyTitle,
        String syncStatus,
        LocalDateTime requestedAt,
        LocalDateTime syncedAt,
        int retryCount,
        String lastError
) {
}
