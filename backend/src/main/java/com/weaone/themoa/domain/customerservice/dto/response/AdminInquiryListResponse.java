package com.weaone.themoa.domain.customerservice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminInquiryListResponse(
        List<AdminInquiryListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static AdminInquiryListResponse from(Page<AdminInquiryListItemResponse> page) {
        return new AdminInquiryListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
