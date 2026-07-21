package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerKnowledgeChunk;

public record AdminCustomerKnowledgeChunkResponse(
        Long id,
        int chunkIndex,
        String qdrantPointId,
        String content
) {
    public static AdminCustomerKnowledgeChunkResponse from(CustomerKnowledgeChunk chunk) {
        return new AdminCustomerKnowledgeChunkResponse(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getQdrantPointId(),
                chunk.getContent());
    }
}
