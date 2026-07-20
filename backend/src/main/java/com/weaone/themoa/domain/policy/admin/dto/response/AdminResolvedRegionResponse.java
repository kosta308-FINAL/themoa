package com.weaone.themoa.domain.policy.admin.dto.response;

public record AdminResolvedRegionResponse(
        String province,
        String city,
        String regionLevel,
        boolean regionExplicit
) {
}
