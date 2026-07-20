package com.weaone.themoa.domain.policy.policy.service;

public record PolicyCollectionResult(
        long runId,
        int totalCount,
        int requestedPages,
        int apiRequestCount,
        int receivedCount,
        int insertedCount,
        int updatedCount,
        int failedCount,
        String status,
        String message
) {
}
