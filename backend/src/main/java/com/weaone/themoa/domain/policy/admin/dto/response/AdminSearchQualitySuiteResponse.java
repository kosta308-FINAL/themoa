package com.weaone.themoa.domain.policy.admin.dto.response;

import java.util.List;

public record AdminSearchQualitySuiteResponse(
        String generatedAt,
        int passed,
        int total,
        boolean success,
        List<AdminSearchQualityCaseResponse> cases
) {
}
