package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryCategory;

public record InquiryCategoryResponse(Long id, String name) {

    public static InquiryCategoryResponse from(CustomerInquiryCategory category) {
        return new InquiryCategoryResponse(category.getId(), category.getName());
    }
}
