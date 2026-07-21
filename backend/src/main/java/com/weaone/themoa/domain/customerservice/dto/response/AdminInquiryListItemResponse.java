package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;

import java.time.LocalDateTime;

public record AdminInquiryListItemResponse(
        Long id,
        String categoryName,
        String title,
        String memberEmail,
        String status,
        LocalDateTime createdAt
) {

    public static AdminInquiryListItemResponse from(CustomerInquiry inquiry) {
        return new AdminInquiryListItemResponse(
                inquiry.getId(),
                inquiry.getInquiryCategory().getName(),
                inquiry.getTitle(),
                inquiry.getMember().getEmail(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }
}
