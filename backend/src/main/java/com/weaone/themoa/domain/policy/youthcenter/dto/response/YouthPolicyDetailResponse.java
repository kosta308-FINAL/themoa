package com.weaone.themoa.domain.policy.youthcenter.dto.response;

import com.weaone.themoa.domain.policy.youthcenter.dto.parsed.SchemaAnalysis;
import com.weaone.themoa.domain.policy.youthcenter.parser.ResponseType;

import java.util.List;

public record YouthPolicyDetailResponse(
        String sourceMode,
        String maskedRequestUrl,
        int statusCode,
        String contentType,
        ResponseType responseType,
        boolean redirected,
        String redirectLocation,
        long elapsedTimeMs,
        int responseLength,
        String responsePreview,
        String rawResponseFilePath,
        YouthPolicyView policy,
        SchemaAnalysis schemaAnalysis,
        List<String> warnings
) {
}
