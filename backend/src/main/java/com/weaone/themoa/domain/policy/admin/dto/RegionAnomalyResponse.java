package com.weaone.themoa.domain.policy.admin.dto;

import com.weaone.themoa.domain.policy.policy.region.RegionEvidence;

import java.util.List;

public record RegionAnomalyResponse(
        Integer policyId,
        String title,
        List<String> currentRegions,
        List<String> resolvedRegions,
        List<RegionEvidence> evidence
) {
}
