package com.weaone.themoa.domain.notification.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/** 읽지 않은 알림 배지 기준(알림.md §A)까지 함께 내려 목록 화면에서 배지를 바로 그릴 수 있게 한다. */
public record NotificationListResponse(
        List<NotificationResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long unreadCount
) {

    public static NotificationListResponse from(Page<NotificationResponse> page, long unreadCount) {
        return new NotificationListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                unreadCount
        );
    }
}
