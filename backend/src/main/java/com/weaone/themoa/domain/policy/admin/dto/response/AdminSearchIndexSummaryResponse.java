package com.weaone.themoa.domain.policy.admin.dto.response;

import java.util.List;

public record AdminSearchIndexSummaryResponse(
        boolean ready,
        long documentCount,
        String projectionVersion,
        long projectionCount,
        long missingSnapshotCount,
        String builtAt,
        String lastProjectionBuiltAt,
        List<String> missingSteps,
        boolean projectionIndexMismatch
) {
}
