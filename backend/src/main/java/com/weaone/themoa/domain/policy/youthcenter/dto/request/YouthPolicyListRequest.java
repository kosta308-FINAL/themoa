package com.weaone.themoa.domain.policy.youthcenter.dto.request;

import com.weaone.themoa.domain.policy.youthcenter.config.YouthCenterApiProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record YouthPolicyListRequest(
        @Min(1) Integer pageNum,
        @Min(1) @Max(100) Integer pageSize,
        String returnType,
        String policyKeywordName,
        String policyDescription
) {
    public int effectivePageNum(YouthCenterApiProperties properties) {
        return pageNum == null ? properties.getDefaultPageNumber() : pageNum;
    }

    public int effectivePageSize(YouthCenterApiProperties properties) {
        int value = pageSize == null ? properties.getDefaultPageSize() : pageSize;
        return Math.min(value, properties.getMaximumPageSize());
    }

    public String effectiveReturnType(YouthCenterApiProperties properties) {
        return returnType == null || returnType.isBlank() ? properties.getDefaultReturnType() : returnType.toLowerCase();
    }
}
