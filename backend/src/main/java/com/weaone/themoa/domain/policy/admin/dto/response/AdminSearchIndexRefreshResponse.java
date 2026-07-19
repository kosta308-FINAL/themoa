package com.weaone.themoa.domain.policy.admin.dto.response;

public record AdminSearchIndexRefreshResponse(
        boolean ready,
        int documentCount,
        String builtAt
) {
}
