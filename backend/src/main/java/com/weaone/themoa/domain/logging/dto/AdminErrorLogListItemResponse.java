package com.weaone.themoa.domain.logging.dto;

import com.weaone.themoa.domain.logging.entity.AiDiagnosisStatus;

import java.time.LocalDateTime;

/** 관리자 에러 목록 한 행(managelogging.md §5-2). StackTrace·AI 긴 본문은 포함하지 않는다. */
public record AdminErrorLogListItemResponse(
        Long id,
        String traceId,
        Long memberId,
        String httpMethod,
        String requestUri,
        String controller,
        int statusCode,
        String exceptionClass,
        String errorMessage,
        AiDiagnosisStatus diagnosisStatus,
        LocalDateTime createdAt
) {
}
