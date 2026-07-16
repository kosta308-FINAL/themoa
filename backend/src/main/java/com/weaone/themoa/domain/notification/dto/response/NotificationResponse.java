package com.weaone.themoa.domain.notification.dto.response;

import com.weaone.themoa.domain.notification.entity.Notification;

import java.time.LocalDateTime;

/** 앱 내 알림 1건(알림.md MOA-S-NOT-APP-02·-05). fixedExpenseId는 딥링크 이동 대상이다. */
public record NotificationResponse(
        Long id,
        String type,
        String message,
        boolean read,
        LocalDateTime readAt,
        Long fixedExpenseId,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getFixedExpense() == null ? null : notification.getFixedExpense().getId(),
                notification.getCreatedAt()
        );
    }
}
