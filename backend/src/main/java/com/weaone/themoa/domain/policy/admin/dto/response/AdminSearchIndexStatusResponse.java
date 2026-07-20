package com.weaone.themoa.domain.policy.admin.dto.response;

import java.util.List;

public record AdminSearchIndexStatusResponse(
        boolean ready,
        long documentCount,
        String projectionVersion,
        long projectionCount,
        long missingSnapshotCount,
        String builtAt,
        List<String> missingSteps
) {
}
