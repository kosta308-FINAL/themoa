package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeCitation;

import java.util.List;

public record CustomerServiceChatResponse(
        Long conversationId,
        String answerMarkdown,
        List<CustomerKnowledgeCitation> citations,
        boolean needsHumanSupport
) {
}
