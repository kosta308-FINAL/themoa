package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;

public record PolicyRecommendationMatch(
        boolean matched,
        int score,
        RegionCompatibility regionCompatibility,
        String matchReason,
        String exclusionReason
) {
    public PolicyRecommendationMatch(boolean matched, int score, RegionCompatibility regionCompatibility,
                                     String matchReason) {
        this(matched, score, regionCompatibility, matchReason, "");
    }

    public static PolicyRecommendationMatch excluded() {
        return excluded("NOT_ELIGIBLE");
    }

    public static PolicyRecommendationMatch excluded(String reason) {
        return new PolicyRecommendationMatch(false, 0, RegionCompatibility.NOT_MATCHED, "", reason);
    }
}
