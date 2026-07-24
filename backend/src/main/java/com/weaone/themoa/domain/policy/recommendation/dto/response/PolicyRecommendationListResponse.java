package com.weaone.themoa.domain.policy.recommendation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PolicyRecommendationListResponse(
        boolean configured,
        LocalDateTime generatedAt,
        PolicyRecommendationProfileSummaryResponse profile,
        List<PolicyRecommendationItemResponse> items
) {
}
