package com.weaone.themoa.domain.policy.admin.dto.response;

public record AdminSearchProjectionRebuildResponse(
        String version,
        long total,
        long processed,
        long missingSnapshot,
        int indexDocumentCount
) {
}
