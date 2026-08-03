package com.weaone.themoa.domain.member.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** {@code GET /api/admin/members/{memberId}/action-logs} 응답. */
public record MemberAdminActionLogListResponse(
        List<MemberAdminActionLogResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public static MemberAdminActionLogListResponse from(Page<MemberAdminActionLogResponse> page) {
        return new MemberAdminActionLogListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
