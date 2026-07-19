package com.weaone.themoa.domain.policy.admin.dto.dashboard;

import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.admin.dto.AdminStatusResponse;
import com.weaone.themoa.domain.policy.admin.dto.SearchReadinessResponse;

import java.util.Map;

public record AdminDashboardResponse(
        AdminStatusResponse status,
        Map<String, Object> searchIndex,
        SearchReadinessResponse readiness,
        AdminJobStatus currentJob
) {
}
