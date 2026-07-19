package com.weaone.themoa.domain.policy.youthcenter.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouthCenterApiEnvelope(
        Integer resultCode,
        String resultMessage,
        YouthCenterApiResult result
) {
}
