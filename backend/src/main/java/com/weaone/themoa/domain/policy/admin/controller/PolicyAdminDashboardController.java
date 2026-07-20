package com.weaone.themoa.domain.policy.admin.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.admin.dto.AdminStatusResponse;
import com.weaone.themoa.domain.policy.admin.dto.dashboard.AdminDashboardResponse;
import com.weaone.themoa.domain.policy.admin.service.AdminDashboardFacade;
import com.weaone.themoa.domain.policy.admin.service.AdminStatusService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.policy.local-tools", name = "enabled", havingValue = "true")
@RequestMapping("/api/policies/admin")
public class PolicyAdminDashboardController {
    private final AdminStatusService statusService;
    private final AdminDashboardFacade dashboardFacade;

    public PolicyAdminDashboardController(AdminStatusService statusService,
                                          AdminDashboardFacade dashboardFacade) {
        this.statusService = statusService;
        this.dashboardFacade = dashboardFacade;
    }

    @GetMapping("/status")
    public ApiResponse<AdminStatusResponse> status() {
        return ApiResponse.success(statusService.status());
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.success(dashboardFacade.dashboard());
    }
}
