package com.weaone.themoa.domain.policy.youthcenter.dto.response;

import com.weaone.themoa.domain.policy.youthcenter.dto.parsed.SchemaAnalysis;
import com.weaone.themoa.domain.policy.youthcenter.parser.ResponseType;

import java.util.List;

public record YouthPolicyListResponse(
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
        boolean listNodeFound,
        String listNodePath,
        int parsedCount,
        Integer totalCount,
        Integer currentPage,
        Integer pageSize,
        YouthPolicyView firstPolicy,
        List<YouthPolicyView> policies,
        SchemaAnalysis schemaAnalysis
) {
}
