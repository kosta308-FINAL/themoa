package com.weaone.themoa.domain.policy.recommendation.service;

public record PolicyRecommendationMatch(
        boolean matched,
        int score,
        String matchReason
) {
    public static PolicyRecommendationMatch excluded() {
        return new PolicyRecommendationMatch(false, 0, "");
    }
}
