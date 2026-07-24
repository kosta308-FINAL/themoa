package com.weaone.themoa.domain.policy.admin.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminPolicyRawResponse;
import com.weaone.themoa.domain.policy.admin.service.AdminPolicyRawService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.policy.local-tools", name = "enabled", havingValue = "true")
@RequestMapping("/api/policies/admin")
public class PolicyAdminRawController {
    private final AdminPolicyRawService rawService;

    public PolicyAdminRawController(AdminPolicyRawService rawService) {
        this.rawService = rawService;
    }

    @GetMapping("/{policyId}/raw")
    public ApiResponse<AdminPolicyRawResponse> raw(@PathVariable Integer policyId) {
        return ApiResponse.success(rawService.raw(policyId));
    }
}
