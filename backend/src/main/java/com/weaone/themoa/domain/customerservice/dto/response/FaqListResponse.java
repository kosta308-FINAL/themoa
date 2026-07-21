package com.weaone.themoa.domain.customerservice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record FaqListResponse(
        List<FaqResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static FaqListResponse from(Page<FaqResponse> page) {
        return new FaqListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
