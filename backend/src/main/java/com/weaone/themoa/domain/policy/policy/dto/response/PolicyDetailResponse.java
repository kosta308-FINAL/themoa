package com.weaone.themoa.domain.policy.policy.dto.response;

import java.util.List;

public record PolicyDetailResponse(
        Integer policyId,
        String sourcePolicyId,
        String title,
        String category,
        String agencyName,
        String summary,
        String officialUrl,
        String status,
        List<String> regions
) {
}
