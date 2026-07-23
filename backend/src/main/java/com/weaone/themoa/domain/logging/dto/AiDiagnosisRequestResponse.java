package com.weaone.themoa.domain.logging.dto;

import com.weaone.themoa.domain.logging.entity.AiDiagnosisStatus;

/** {@code POST /api/admin/logs/errors/{errorLogId}/ai-analyze} 응답(managelogging.md §5-4). 항상 202로 감싼다. */
public record AiDiagnosisRequestResponse(
        Long diagnosisId,
        Long errorLogId,
        AiDiagnosisStatus status
) {
}
