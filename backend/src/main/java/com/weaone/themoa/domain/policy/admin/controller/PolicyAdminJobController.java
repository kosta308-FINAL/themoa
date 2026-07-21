package com.weaone.themoa.domain.policy.admin.controller;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.admin.service.AdminJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies/admin")
public class PolicyAdminJobController {
    private final AdminJobService jobService;

    public PolicyAdminJobController(AdminJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs/policy-collection")
    public ResponseEntity<ApiResponse<AdminJobStatus>> collect() {
        return accepted(jobService.start("POLICY_COLLECTION"));
    }

    @PostMapping("/jobs/policy-sync")
    public ResponseEntity<ApiResponse<AdminJobStatus>> syncPolicies() {
        return accepted(jobService.start("POLICY_SYNC"));
    }

    @PostMapping("/jobs/embedding-queue")
    public ResponseEntity<ApiResponse<AdminJobStatus>> queue() {
        return accepted(jobService.start("EMBEDDING_QUEUE"));
    }

    @PostMapping("/jobs/embedding-process")
    public ResponseEntity<ApiResponse<AdminJobStatus>> process() {
        return accepted(jobService.start("EMBEDDING_PROCESS"));
    }

    @PostMapping("/jobs/embedding-retry-failed")
    public ResponseEntity<ApiResponse<AdminJobStatus>> retry() {
        return accepted(jobService.start("EMBEDDING_RETRY_FAILED"));
    }

    @PostMapping("/jobs/policy-region-rebuild")
    public ResponseEntity<ApiResponse<AdminJobStatus>> rebuildRegions() {
        return accepted(jobService.start("POLICY_REGION_REBUILD"));
    }

    @PostMapping("/jobs/region-catalog-sync")
    public ResponseEntity<ApiResponse<AdminJobStatus>> syncRegionCatalog() {
        return accepted(jobService.start("REGION_CATALOG_SYNC"));
    }

    @PostMapping("/jobs/region-catalog-repair")
    public ResponseEntity<ApiResponse<AdminJobStatus>> repairRegionCatalog() {
        return accepted(jobService.start("REGION_CATALOG_REPAIR"));
    }

    @PostMapping("/jobs/search-projection-rebuild")
    public ResponseEntity<ApiResponse<AdminJobStatus>> rebuildSearchProjectionJob() {
        return accepted(jobService.start("SEARCH_PROJECTION_REBUILD"));
    }

    @PostMapping("/jobs/search-index-refresh")
    public ResponseEntity<ApiResponse<AdminJobStatus>> refreshSearchIndexJob() {
        return accepted(jobService.start("SEARCH_INDEX_REFRESH"));
    }

    @PostMapping("/jobs/full-reindex")
    public ResponseEntity<ApiResponse<AdminJobStatus>> fullReindex() {
        return accepted(jobService.start("FULL_REINDEX"));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<AdminJobStatus> job(@PathVariable String jobId) {
        return ApiResponse.success(jobService.find(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_JOB_NOT_FOUND)));
    }

    @GetMapping("/jobs/latest")
    public ApiResponse<AdminJobStatus> latest() {
        return ApiResponse.success(jobService.latest().orElse(null));
    }

    private ResponseEntity<ApiResponse<AdminJobStatus>> accepted(AdminJobStatus status) {
        return ResponseEntity.accepted().body(ApiResponse.success(status));
    }
}
