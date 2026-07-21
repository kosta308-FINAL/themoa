package com.weaone.themoa.domain.customerservice.dto.response;

import java.util.List;

public record AdminCustomerKnowledgeChunkPreviewResponse(
        int chunkCount,
        int totalLength,
        List<Item> chunks
) {

    public record Item(
            int chunkIndex,
            int length,
            String content
    ) {
    }
}
