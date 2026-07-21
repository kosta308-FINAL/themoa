package com.weaone.themoa.domain.policy.bookmark.dto.response;

import com.weaone.themoa.domain.policy.bookmark.entity.PolicyBookmark;
import com.weaone.themoa.domain.policy.policy.entity.Policy;

import java.time.LocalDate;

public record PolicyBookmarkResponse(
        Integer bookmarkId,
        Integer policyId,
        String title,
        String category,
        String agencyName,
        String summary,
        String officialUrl,
        LocalDate startDate,
        LocalDate dueDate,
        boolean alwaysOpen,
        boolean active,
        String policyStatus,
        String applyStatus,
        boolean notificationEnabled,
        String note
) {
    public static PolicyBookmarkResponse from(PolicyBookmark bookmark) {
        Policy policy = bookmark.getPolicy();
        return new PolicyBookmarkResponse(
                bookmark.getId(),
                policy.getId(),
                policy.getTitle(),
                policy.getCategory().name(),
                policy.getAgencyName(),
                policy.getSummary() == null ? "" : policy.getSummary(),
                policy.getOfficialUrl() == null ? "" : policy.getOfficialUrl(),
                policy.getStartDate(),
                policy.getDueDate(),
                policy.isAlwaysOpen(),
                policy.isActive(),
                policy.getStatus(),
                bookmark.getApplyStatus().name(),
                bookmark.isNotificationEnabled(),
                bookmark.getNote()
        );
    }
}
