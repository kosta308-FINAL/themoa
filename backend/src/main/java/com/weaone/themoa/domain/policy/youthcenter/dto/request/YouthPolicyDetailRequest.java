package com.weaone.themoa.domain.policy.youthcenter.dto.request;

import com.weaone.themoa.domain.policy.youthcenter.config.YouthCenterApiProperties;
import jakarta.validation.constraints.NotBlank;

public record YouthPolicyDetailRequest(
        @NotBlank String policyNumber,
        String returnType
) {
    public String effectiveReturnType(YouthCenterApiProperties properties) {
        return returnType == null || returnType.isBlank() ? properties.getDefaultReturnType() : returnType.toLowerCase();
    }
}
