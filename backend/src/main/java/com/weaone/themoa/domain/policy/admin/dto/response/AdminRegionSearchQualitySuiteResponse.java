package com.weaone.themoa.domain.policy.admin.dto.response;

import java.util.List;

public record AdminRegionSearchQualitySuiteResponse(
        String generatedAt,
        int passed,
        int total,
        boolean success,
        List<String> failedRegions,
        List<AdminRegionSearchQualityCaseResponse> cases
) {
}
