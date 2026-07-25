package com.weaone.themoa.domain.customerservice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUnansweredQuestionListResponse(
        List<AdminUnansweredQuestionItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long newCount
) {

    public static AdminUnansweredQuestionListResponse from(Page<AdminUnansweredQuestionItemResponse> page,
                                                            long newCount) {
        return new AdminUnansweredQuestionListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                newCount
        );
    }
}
