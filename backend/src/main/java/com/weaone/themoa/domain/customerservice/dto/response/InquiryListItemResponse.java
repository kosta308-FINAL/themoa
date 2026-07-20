package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;

import java.time.LocalDateTime;

/** 목록에는 본문 전체·답변 전체를 넣지 않는다(customerservice.md §4-2). */
public record InquiryListItemResponse(
        Long id,
        String categoryName,
        String title,
        String status,
        LocalDateTime createdAt
) {

    public static InquiryListItemResponse from(CustomerInquiry inquiry) {
        return new InquiryListItemResponse(
                inquiry.getId(),
                inquiry.getInquiryCategory().getName(),
                inquiry.getTitle(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }
}
