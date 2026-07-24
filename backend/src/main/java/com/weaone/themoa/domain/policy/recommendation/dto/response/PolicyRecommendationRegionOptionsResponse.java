package com.weaone.themoa.domain.policy.recommendation.dto.response;

import java.util.List;

public record PolicyRecommendationRegionOptionsResponse(
        List<PolicyRecommendationSidoOptionResponse> items
) {
}
