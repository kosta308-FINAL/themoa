package com.weaone.themoa.domain.policy.policy.region;

public record RegionEvidence(
        RegionEvidenceSource source,
        String rawValue,
        String matchedRegion,
        int confidence
) {
}
