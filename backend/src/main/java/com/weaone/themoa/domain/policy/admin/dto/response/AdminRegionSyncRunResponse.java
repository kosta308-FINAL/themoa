package com.weaone.themoa.domain.policy.admin.dto.response;

import java.time.LocalDateTime;

public record AdminRegionSyncRunResponse(
        Long id,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        int apiProvinceCount,
        int apiMunicipalityCount,
        int insertedCount,
        int updatedCount,
        int unchangedCount,
        int failedCount,
        int progressPercent,
        String currentProvinceCode,
        String currentProvinceName,
        String errorSummary
) {
}
