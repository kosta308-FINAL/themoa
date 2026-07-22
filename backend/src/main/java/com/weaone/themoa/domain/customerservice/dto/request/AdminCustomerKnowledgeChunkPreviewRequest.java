package com.weaone.themoa.domain.customerservice.dto.request;

public record AdminCustomerKnowledgeChunkPreviewRequest(
        String content,
        Integer chunkMaxLength,
        Integer chunkOverlapLength,
        Boolean splitByMarkdownHeading,
        Boolean splitByParagraph
) {
}
