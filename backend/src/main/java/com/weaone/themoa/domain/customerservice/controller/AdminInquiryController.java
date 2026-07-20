package com.weaone.themoa.domain.customerservice.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.customerservice.dto.request.InquiryAnswerRequest;
import com.weaone.themoa.domain.customerservice.dto.response.AdminInquiryDetailResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminInquiryListResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryAnswerResponse;
import com.weaone.themoa.domain.customerservice.entity.InquiryStatus;
import com.weaone.themoa.domain.customerservice.service.AdminInquiryService;
import com.weaone.themoa.domain.customerservice.support.DownloadableAttachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** 관리자 1:1 문의 관리(customerservice.md §4-3). {@code SecurityConfig}가 ROLE_ADMIN만 통과시킨다. */
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @Operation(summary = "문의 목록(관리자)", description = "status·inquiryCategoryId·keyword로 필터링합니다. PENDING 우선, 그 안에서 오래된 순입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminInquiryListResponse>> list(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Long inquiryCategoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        AdminInquiryListResponse response = adminInquiryService.list(status, inquiryCategoryId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "문의 상세(관리자)")
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<AdminInquiryDetailResponse>> detail(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(adminInquiryService.detail(inquiryId)));
    }

    @Operation(summary = "첨부파일 다운로드(관리자)")
    @GetMapping("/{inquiryId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long inquiryId, @PathVariable Long attachmentId) {
        DownloadableAttachment file = adminInquiryService.downloadAttachment(inquiryId, attachmentId);
        String encodedFilename = java.net.URLEncoder.encode(file.originalFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(file.content());
    }

    @Operation(summary = "답변 최초 등록 또는 수정",
            description = "답변이 없으면 version을 생략하거나 null로 보내 최초 등록합니다. 수정은 상세 응답의 현재 version을 그대로 보내야 하며, 오래된 version이면 409입니다.")
    @PutMapping("/{inquiryId}/answer")
    public ResponseEntity<ApiResponse<InquiryAnswerResponse>> upsertAnswer(
            @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request) {
        InquiryAnswerResponse response = adminInquiryService.upsertAnswer(adminId, inquiryId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
