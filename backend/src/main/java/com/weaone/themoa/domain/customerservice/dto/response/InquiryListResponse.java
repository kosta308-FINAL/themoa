package com.weaone.themoa.domain.customerservice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record InquiryListResponse(
        List<InquiryListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static InquiryListResponse from(Page<InquiryListItemResponse> page) {
        return new InquiryListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
