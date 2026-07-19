package com.weaone.themoa.domain.policy.policy.region;

public record RegionEligiblePolicyCandidate(
        Integer policyId,
        RegionCompatibility compatibility
) {
}
