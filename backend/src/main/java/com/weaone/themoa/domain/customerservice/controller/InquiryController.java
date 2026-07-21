package com.weaone.themoa.domain.customerservice.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.customerservice.dto.request.InquiryCreateRequest;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryCategoryResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryDetailResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryListResponse;
import com.weaone.themoa.domain.customerservice.service.InquiryService;
import com.weaone.themoa.domain.customerservice.support.DownloadableAttachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 회원 1:1 문의 접수·조회(customerservice.md §4-2). 전부 로그인 사용자 전용이다. */
@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "활성 문의 카테고리 목록")
    @GetMapping("/api/inquiry-categories")
    public ResponseEntity<ApiResponse<List<InquiryCategoryResponse>>> categories() {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.listCategories()));
    }

    @Operation(summary = "1:1 문의 등록", description = "request JSON과 이미지 첨부(최대 3개, PNG/JPEG)를 함께 받습니다.")
    @PostMapping(value = "/api/inquiries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestPart("request") InquiryCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        InquiryDetailResponse response = inquiryService.create(memberId, request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "내 문의 목록")
    @GetMapping("/api/inquiries")
    public ResponseEntity<ApiResponse<InquiryListResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.list(memberId, page, size)));
    }

    @Operation(summary = "내 문의 상세·답변·첨부 메타데이터")
    @GetMapping("/api/inquiries/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> detail(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.detail(memberId, inquiryId)));
    }

    @Operation(summary = "본인 첨부파일 다운로드")
    @GetMapping("/api/inquiries/{inquiryId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long inquiryId,
            @PathVariable Long attachmentId) {
        DownloadableAttachment file = inquiryService.downloadAttachment(memberId, inquiryId, attachmentId);
        String encodedFilename = java.net.URLEncoder.encode(file.originalFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(file.content());
    }
}
