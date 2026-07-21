package com.weaone.themoa.domain.policy.admin.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.admin.dto.RegionAnomalyResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionCoverageResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionResolveResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionSearchResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionSyncRunResponse;
import com.weaone.themoa.domain.policy.admin.service.AdminRegionDiagnosticsService;
import com.weaone.themoa.domain.policy.admin.service.AdminRegionQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/policies/admin")
public class PolicyAdminRegionController {
    private final AdminRegionDiagnosticsService regionDiagnosticsService;
    private final AdminRegionQueryService regionQueryService;

    public PolicyAdminRegionController(AdminRegionDiagnosticsService regionDiagnosticsService,
                                       AdminRegionQueryService regionQueryService) {
        this.regionDiagnosticsService = regionDiagnosticsService;
        this.regionQueryService = regionQueryService;
    }

    @GetMapping("/regions/anomalies")
    public ApiResponse<List<RegionAnomalyResponse>> regionAnomalies() {
        return ApiResponse.success(regionDiagnosticsService.anomalies());
    }

    @GetMapping("/regions/resolve")
    public ApiResponse<AdminRegionResolveResponse> resolveRegion(@RequestParam("q") String query) {
        return ApiResponse.success(regionQueryService.resolveRegion(query));
    }

    @GetMapping("/regions/search")
    public ApiResponse<List<AdminRegionSearchResponse>> searchRegions(@RequestParam("name") String name) {
        return ApiResponse.success(regionQueryService.searchRegions(name));
    }

    @GetMapping("/regions/coverage")
    public ApiResponse<AdminRegionCoverageResponse> regionCoverage() {
        return ApiResponse.success(regionQueryService.regionCoverage());
    }

    @GetMapping("/regions/sync-runs/latest")
    public ApiResponse<AdminRegionSyncRunResponse> latestRegionSyncRun() {
        return ApiResponse.success(regionQueryService.latestRegionSyncRun());
    }

    @PostMapping("/regions/cache/refresh")
    public ApiResponse<String> refreshRegionCache() {
        regionQueryService.refreshRegionCache();
        return ApiResponse.success("지역 카탈로그 캐시를 갱신했습니다.");
    }
}
