package com.weaone.themoa.domain.logging.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.logging.dto.AdminErrorLogDetailResponse;
import com.weaone.themoa.domain.logging.dto.AdminErrorLogListResponse;
import com.weaone.themoa.domain.logging.dto.AiDiagnosisRequestResponse;
import com.weaone.themoa.domain.logging.service.AdminErrorLogService;
import com.weaone.themoa.domain.logging.service.AiDiagnosisCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 관리자 에러 목록·상세·수동 AI 분석(managelogging.md §5). {@code SecurityConfig}가 ROLE_ADMIN만 통과시킨다. */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminErrorLogController {

    private final AdminErrorLogService adminErrorLogService;
    private final AiDiagnosisCommandService aiDiagnosisCommandService;

    @Operation(summary = "에러 목록(관리자)",
            description = "exceptionClass·requestUri·controller·memberId는 정확히 일치, startAt/endAt은 [startAt, endAt) 범위입니다.")
    @GetMapping("/errors")
    public ResponseEntity<ApiResponse<AdminErrorLogListResponse>> list(
            @RequestParam(required = false) String exceptionClass,
            @RequestParam(required = false) String requestUri,
            @RequestParam(required = false) String controller,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        AdminErrorLogListResponse response = adminErrorLogService.list(
                exceptionClass, requestUri, controller, memberId, startAt, endAt, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "에러 상세(관리자)", description = "현재 AI 진단 한 건을 함께 반환합니다. 분석 요청 이력이 없으면 aiDiagnosis는 null입니다.")
    @GetMapping("/errors/{errorLogId}")
    public ResponseEntity<ApiResponse<AdminErrorLogDetailResponse>> detail(@PathVariable Long errorLogId) {
        return ResponseEntity.ok(ApiResponse.success(adminErrorLogService.detail(errorLogId)));
    }

    @Operation(summary = "수동 AI 분석 요청(관리자)",
            description = "완료를 기다리지 않고 202를 반환합니다. PENDING 중 재요청은 새 작업을 만들지 않고 기존 PENDING을 반환합니다.")
    @PostMapping("/errors/{errorLogId}/ai-analyze")
    public ResponseEntity<ApiResponse<AiDiagnosisRequestResponse>> requestAiAnalysis(
            @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
            @PathVariable Long errorLogId) {
        AiDiagnosisRequestResponse response = aiDiagnosisCommandService.requestAnalysis(errorLogId, adminId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }
}
