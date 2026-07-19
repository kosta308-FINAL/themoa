package com.weaone.themoa.domain.policy.admin.dto.dashboard;

import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.admin.dto.AdminStatusResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchIndexSummaryResponse;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;

public record AdminDashboardResponse(
        AdminStatusResponse status,
        AdminSearchIndexSummaryResponse searchIndex,
        SearchReadinessResponse readiness,
        AdminJobStatus currentJob
) {
}
