package com.weaone.themoa.domain.member.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** {@code GET /api/admin/members} 응답. */
public record AdminMemberListResponse(
        List<AdminMemberListItemResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public static AdminMemberListResponse from(Page<AdminMemberListItemResponse> page) {
        return new AdminMemberListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
