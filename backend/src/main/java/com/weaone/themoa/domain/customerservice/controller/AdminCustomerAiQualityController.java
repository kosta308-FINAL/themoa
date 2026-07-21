package com.weaone.themoa.domain.customerservice.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.customerservice.dto.request.AdminCustomerAiPreviewRequest;
import com.weaone.themoa.domain.customerservice.dto.request.AdminCustomerAiSearchRequest;
import com.weaone.themoa.domain.customerservice.dto.request.AdminCustomerAiSettingsRequest;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerAiPreviewResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerAiSearchResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerAiSettingsResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerKnowledgeFileResponse;
import com.weaone.themoa.domain.customerservice.service.AdminCustomerAiQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customer-service/ai-quality")
@RequiredArgsConstructor
public class AdminCustomerAiQualityController {

    private final AdminCustomerAiQualityService aiQualityService;

    @GetMapping("/settings")
    public ApiResponse<AdminCustomerAiSettingsResponse> settings() {
        return ApiResponse.success(aiQualityService.settings());
    }

    @PutMapping("/settings")
    public ApiResponse<AdminCustomerAiSettingsResponse> updateSettings(
            @AuthenticationPrincipal Long adminId,
            @RequestBody AdminCustomerAiSettingsRequest request) {
        return ApiResponse.success(aiQualityService.updateSettings(adminId, request));
    }

    @PostMapping("/search")
    public ApiResponse<AdminCustomerAiSearchResponse> search(@RequestBody AdminCustomerAiSearchRequest request) {
        return ApiResponse.success(aiQualityService.search(request));
    }

    @PostMapping("/preview")
    public ApiResponse<AdminCustomerAiPreviewResponse> preview(@RequestBody AdminCustomerAiPreviewRequest request) {
        return ApiResponse.success(aiQualityService.preview(request));
    }

    @GetMapping("/documents")
    public ApiResponse<List<AdminCustomerKnowledgeFileResponse>> documents() {
        return ApiResponse.success(aiQualityService.documents());
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminCustomerKnowledgeFileResponse> upload(
            @AuthenticationPrincipal Long adminId,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(aiQualityService.upload(adminId, title, category, file));
    }

    @PostMapping("/documents/{documentId}/reembed")
    public ApiResponse<AdminCustomerKnowledgeFileResponse> reembed(@PathVariable Long documentId) {
        return ApiResponse.success(aiQualityService.reembed(documentId));
    }

    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> disable(@PathVariable Long documentId) {
        aiQualityService.disable(documentId);
        return ApiResponse.success(null);
    }
}
