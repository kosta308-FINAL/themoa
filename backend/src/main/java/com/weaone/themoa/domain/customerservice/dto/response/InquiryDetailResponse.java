package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryDetailResponse(
        Long id,
        Long inquiryCategoryId,
        String categoryName,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        List<AttachmentResponse> attachments,
        InquiryAnswerResponse answer
) {

    public static InquiryDetailResponse of(CustomerInquiry inquiry, List<AttachmentResponse> attachments,
                                            InquiryAnswerResponse answer) {
        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getInquiryCategory().getId(),
                inquiry.getInquiryCategory().getName(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt(),
                attachments,
                answer
        );
    }
}
