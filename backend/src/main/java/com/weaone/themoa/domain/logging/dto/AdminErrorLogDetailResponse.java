package com.weaone.themoa.domain.logging.dto;

import com.weaone.themoa.domain.logging.entity.AiDiagnosisStatus;
import com.weaone.themoa.domain.logging.entity.AiLogDiagnosis;
import com.weaone.themoa.domain.logging.entity.ErrorLog;

import java.time.LocalDateTime;

/** {@code GET /api/admin/logs/errors/{errorLogId}} 응답(managelogging.md §5-3). */
public record AdminErrorLogDetailResponse(
        ErrorLogDetail errorLog,
        AiDiagnosisDetail aiDiagnosis
) {

    public record ErrorLogDetail(
            Long id,
            String traceId,
            Long memberId,
            String httpMethod,
            String requestUri,
            String controller,
            int statusCode,
            String exceptionClass,
            String errorMessage,
            String stackTraceExcerpt,
            LocalDateTime createdAt
    ) {
        public static ErrorLogDetail from(ErrorLog errorLog) {
            return new ErrorLogDetail(
                    errorLog.getId(),
                    errorLog.getTraceId(),
                    errorLog.getMemberId(),
                    errorLog.getHttpMethod(),
                    errorLog.getRequestUri(),
                    errorLog.getController(),
                    errorLog.getStatusCode(),
                    errorLog.getExceptionClass(),
                    errorLog.getErrorMessage(),
                    errorLog.getStackTraceExcerpt(),
                    errorLog.getCreatedAt()
            );
        }
    }

    /** 요청 관리자가 탈퇴했으면 {@code requestedByMemberId}는 {@code ON DELETE SET NULL}로 null일 수 있다. */
    public record AiDiagnosisDetail(
            Long id,
            AiDiagnosisStatus status,
            Long requestedByMemberId,
            String causeCategory,
            String summary,
            String rootCause,
            String recommendedAction,
            String modelName,
            String failureMessage,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        public static AiDiagnosisDetail from(AiLogDiagnosis diagnosis) {
            return new AiDiagnosisDetail(
                    diagnosis.getId(),
                    diagnosis.getStatus(),
                    diagnosis.getRequestedByMemberId(),
                    diagnosis.getCauseCategory(),
                    diagnosis.getSummary(),
                    diagnosis.getRootCause(),
                    diagnosis.getRecommendedAction(),
                    diagnosis.getModelName(),
                    diagnosis.getFailureMessage(),
                    diagnosis.getCreatedAt(),
                    diagnosis.getCompletedAt()
            );
        }
    }

    public static AdminErrorLogDetailResponse of(ErrorLog errorLog, AiLogDiagnosis diagnosis) {
        return new AdminErrorLogDetailResponse(
                ErrorLogDetail.from(errorLog),
                diagnosis == null ? null : AiDiagnosisDetail.from(diagnosis)
        );
    }
}
