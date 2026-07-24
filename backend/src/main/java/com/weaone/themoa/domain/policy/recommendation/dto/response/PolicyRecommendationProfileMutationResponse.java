package com.weaone.themoa.domain.policy.recommendation.dto.response;

public record PolicyRecommendationProfileMutationResponse(
        PolicyRecommendationProfileResponse profile,
        PolicyRecommendationListResponse recommendations,
        boolean recommendationRefreshed,
        String recommendationMessage
) {
}
