package com.weaone.themoa.domain.policy.admin.dto.embedding;

import java.util.List;

public record AdminEmbeddingPageResponse(
        List<AdminEmbeddingItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
