package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;

public record PolicyRecommendationMatch(
        boolean matched,
        int score,
        RegionCompatibility regionCompatibility,
        String matchReason
) {
    public static PolicyRecommendationMatch excluded() {
        return new PolicyRecommendationMatch(false, 0, RegionCompatibility.NOT_MATCHED, "");
    }
}
