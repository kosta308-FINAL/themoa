package com.weaone.themoa.domain.policy.admin.dto.response;

import java.util.List;

public record AdminRegionSearchQualityCaseResponse(
        String query,
        AdminResolvedRegionResponse resolvedRegion,
        int eligiblePoolCount,
        long exactCount,
        long parentCount,
        long nationwideCount,
        long multipleRegionPoolCount,
        int unknownExcludedCount,
        int wrongRegionExcludedCount,
        List<String> top20RegionCompatibility,
        List<String> violations,
        boolean passed
) {
}
