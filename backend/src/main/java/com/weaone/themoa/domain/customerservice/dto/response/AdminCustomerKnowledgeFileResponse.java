package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerKnowledgeFile;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCustomerKnowledgeFileResponse(
        Long id,
        String title,
        String category,
        String originalFilename,
        long fileSize,
        int chunkCount,
        int chunkMaxLength,
        int chunkMinLength,
        int chunkOverlapLength,
        boolean splitByMarkdownHeading,
        boolean splitByParagraph,
        String status,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime embeddedAt,
        List<AdminCustomerKnowledgeChunkResponse> chunks
) {
    public static AdminCustomerKnowledgeFileResponse of(CustomerKnowledgeFile file,
                                                        List<AdminCustomerKnowledgeChunkResponse> chunks) {
        return new AdminCustomerKnowledgeFileResponse(
                file.getId(),
                file.getTitle(),
                file.getCategory(),
                file.getOriginalFilename(),
                file.getFileSize(),
                file.getChunkCount(),
                file.getChunkMaxLength(),
                file.getChunkMinLength(),
                file.getChunkOverlapLength(),
                file.isSplitByMarkdownHeading(),
                file.isSplitByParagraph(),
                file.getStatus().name(),
                file.isActive(),
                file.getCreatedAt(),
                file.getEmbeddedAt(),
                chunks);
    }
}
