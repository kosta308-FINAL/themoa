package com.weaone.themoa.domain.policy.bookmark.dto.response;

import java.util.List;

public record PolicyBookmarkListResponse(
        List<PolicyBookmarkResponse> items
) {
}
