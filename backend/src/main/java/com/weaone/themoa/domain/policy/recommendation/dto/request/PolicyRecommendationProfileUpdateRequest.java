package com.weaone.themoa.domain.policy.recommendation.dto.request;

import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PolicyRecommendationProfileUpdateRequest(
        @NotBlank
        String residenceSido,

        String residenceSigungu,

        @NotNull
        UserEmploymentStatus employmentStatus
) {
}
