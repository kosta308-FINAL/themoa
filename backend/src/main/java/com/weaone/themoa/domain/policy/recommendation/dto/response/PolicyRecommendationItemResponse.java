package com.weaone.themoa.domain.policy.recommendation.dto.response;

import java.time.LocalDate;

public record PolicyRecommendationItemResponse(
        Integer policyId,
        String sourcePolicyId,
        String title,
        String category,
        String region,
        String agencyName,
        String summary,
        Integer minAge,
        Integer maxAge,
        String employmentStatus,
        LocalDate startDate,
        LocalDate dueDate,
        boolean alwaysOpen,
        String applicationStatus,
        String officialUrl,
        int score,
        String matchReason
) {
}
