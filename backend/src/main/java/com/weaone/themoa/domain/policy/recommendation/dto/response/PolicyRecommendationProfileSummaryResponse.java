package com.weaone.themoa.domain.policy.recommendation.dto.response;

import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;

import java.time.LocalDate;

public record PolicyRecommendationProfileSummaryResponse(
        LocalDate birthDate,
        int age,
        String residenceSido,
        String residenceSigungu,
        UserEmploymentStatus employmentStatus
) {
}
