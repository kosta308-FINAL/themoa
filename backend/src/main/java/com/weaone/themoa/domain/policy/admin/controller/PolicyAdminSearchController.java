package com.weaone.themoa.domain.policy.admin.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.admin.dto.request.AdminSearchExplainRequest;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionSearchQualitySuiteResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchExplainResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchIndexRefreshResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchIndexStatusResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchProjectionRebuildResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchQualitySuiteResponse;
import com.weaone.themoa.domain.policy.admin.service.AdminSearchDiagnosticService;
import com.weaone.themoa.domain.policy.admin.service.AdminSearchIndexService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies/admin")
public class PolicyAdminSearchController {
    private final AdminSearchIndexService searchIndexService;
    private final AdminSearchDiagnosticService searchDiagnosticService;

    public PolicyAdminSearchController(AdminSearchIndexService searchIndexService,
                                       AdminSearchDiagnosticService searchDiagnosticService) {
        this.searchIndexService = searchIndexService;
        this.searchDiagnosticService = searchDiagnosticService;
    }

    @PostMapping("/search/explain")
    public ApiResponse<AdminSearchExplainResponse> explainSearch(@RequestBody AdminSearchExplainRequest request) {
        return ApiResponse.success(searchDiagnosticService.explainSearch(request));
    }

    @PostMapping("/search-projection/rebuild")
    public ApiResponse<AdminSearchProjectionRebuildResponse> rebuildSearchProjection() {
        return ApiResponse.success(searchIndexService.rebuildSearchProjection());
    }

    @PostMapping("/search-index/refresh")
    public ApiResponse<AdminSearchIndexRefreshResponse> refreshSearchIndex() {
        return ApiResponse.success(searchIndexService.refreshSearchIndex());
    }

    @GetMapping("/search-index/status")
    public ApiResponse<AdminSearchIndexStatusResponse> searchIndexStatus() {
        return ApiResponse.success(searchIndexService.searchIndexStatus());
    }

    @PostMapping("/search/quality-suite")
    public ApiResponse<AdminSearchQualitySuiteResponse> searchQualitySuite() {
        return ApiResponse.success(searchDiagnosticService.searchQualitySuite());
    }

    @PostMapping("/search/region-quality-suite")
    public ApiResponse<AdminRegionSearchQualitySuiteResponse> regionSearchQualitySuite() {
        return ApiResponse.success(searchDiagnosticService.regionSearchQualitySuite());
    }
}
