package com.weaone.themoa.domain.policy.admin.dto.response;

import java.util.List;

public record AdminSearchQualityCaseResponse(
        String query,
        boolean passed,
        Object queryType,
        long totalMatched,
        boolean hasNext,
        List<String> regionViolations,
        List<String> topTitles
) {
}
