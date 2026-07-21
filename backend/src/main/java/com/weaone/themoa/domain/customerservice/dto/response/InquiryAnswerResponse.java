package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAnswer;

import java.time.LocalDateTime;

public record InquiryAnswerResponse(
        String contentMarkdown,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InquiryAnswerResponse from(CustomerInquiryAnswer answer) {
        return new InquiryAnswerResponse(
                answer.getContentMarkdown(),
                answer.getVersion(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}
