package com.weaone.themoa.domain.logging.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** {@code GET /api/admin/logs/errors} 응답(managelogging.md §5-2). */
public record AdminErrorLogListResponse(
        List<AdminErrorLogListItemResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public static AdminErrorLogListResponse from(Page<AdminErrorLogListItemResponse> page) {
        return new AdminErrorLogListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
