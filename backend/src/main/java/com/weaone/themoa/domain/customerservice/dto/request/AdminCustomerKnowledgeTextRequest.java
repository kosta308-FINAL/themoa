package com.weaone.themoa.domain.customerservice.dto.request;

public record AdminCustomerKnowledgeTextRequest(
        String title,
        String category,
        String content,
        Integer chunkMaxLength,
        Integer chunkMinLength,
        Integer chunkOverlapLength,
        Boolean splitByMarkdownHeading,
        Boolean splitByParagraph
) {
}
