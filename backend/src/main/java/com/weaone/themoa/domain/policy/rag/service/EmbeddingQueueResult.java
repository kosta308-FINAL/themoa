package com.weaone.themoa.domain.policy.rag.service;

public record EmbeddingQueueResult(
        long activePolicyCount,
        int newlyQueuedCount,
        int requeuedCount,
        int unchangedCount,
        long pendingCountAfter
) {
}
