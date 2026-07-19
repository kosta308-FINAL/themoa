package com.weaone.themoa.domain.policy.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminPolicyRawDataResponse(
        Long rawDataId,
        String requestUrl,
        String responseFormat,
        String collectedAt
) {
}
