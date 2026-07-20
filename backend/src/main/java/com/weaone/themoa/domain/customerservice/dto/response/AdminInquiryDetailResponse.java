package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;

import java.time.LocalDateTime;
import java.util.List;

public record AdminInquiryDetailResponse(
        Long id,
        String categoryName,
        String title,
        String content,
        String memberEmail,
        String status,
        LocalDateTime createdAt,
        List<AttachmentResponse> attachments,
        InquiryAnswerResponse answer
) {

    public static AdminInquiryDetailResponse of(CustomerInquiry inquiry, List<AttachmentResponse> attachments,
                                                 InquiryAnswerResponse answer) {
        return new AdminInquiryDetailResponse(
                inquiry.getId(),
                inquiry.getInquiryCategory().getName(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getMember().getEmail(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt(),
                attachments,
                answer
        );
    }
}
