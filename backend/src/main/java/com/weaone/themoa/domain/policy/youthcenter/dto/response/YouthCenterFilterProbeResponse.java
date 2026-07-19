package com.weaone.themoa.domain.policy.youthcenter.dto.response;

import com.weaone.themoa.domain.policy.youthcenter.parser.ResponseType;

public record YouthCenterFilterProbeResponse(
        String filterType,
        String filterValue,
        String maskedRequestUrl,
        int statusCode,
        String contentType,
        ResponseType responseType,
        int parsedCount,
        Integer totalCount,
        long elapsedTimeMs,
        String errorMessage
) {
}
