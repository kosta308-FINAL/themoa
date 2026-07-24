package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerServiceUnansweredQuestion;

import java.time.LocalDateTime;

public record AdminUnansweredQuestionItemResponse(
        Long id,
        String memberEmail,
        Long conversationId,
        String question,
        String reason,
        String answerMarkdown,
        String status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {

    public static AdminUnansweredQuestionItemResponse from(CustomerServiceUnansweredQuestion question) {
        return new AdminUnansweredQuestionItemResponse(
                question.getId(),
                question.getMember().getEmail(),
                question.getConversationId(),
                question.getQuestion(),
                question.getReason().name(),
                question.getAnswerMarkdown(),
                question.getStatus().name(),
                question.getCreatedAt(),
                question.getResolvedAt()
        );
    }
}
