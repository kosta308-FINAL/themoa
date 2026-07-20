package com.weaone.themoa.domain.policy.policy.region;

public record RegionCandidate(
        Integer regionId,
        String province,
        String city,
        String displayName,
        String matchedAlias,
        RegionTextMatchType matchType
) {
}
