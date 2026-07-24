package com.weaone.themoa.domain.policy.recommendation.dto.response;

import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PolicyRecommendationProfileResponse(
        boolean configured,
        LocalDate birthDate,
        int age,
        String residenceSido,
        String residenceSigungu,
        UserEmploymentStatus employmentStatus,
        LocalDateTime updatedAt
) {
}
