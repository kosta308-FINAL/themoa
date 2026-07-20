package com.weaone.themoa.domain.notification.dto.response;

import com.weaone.themoa.domain.notification.entity.Notification;

import java.time.LocalDateTime;

/**
 * 앱 내 알림 1건(알림.md MOA-S-NOT-APP-02·-05). fixedExpenseId·customerInquiryId는 딥링크 이동 대상이다.
 * {@code type}은 DB 구조와 무관하게 기존 프론트 계약을 유지하도록 {@code notification_type.code}를 내려준다
 * (customerservice.md §7).
 */
public record NotificationResponse(
        Long id,
        String type,
        String message,
        boolean read,
        LocalDateTime readAt,
        Long fixedExpenseId,
        Long customerInquiryId,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType().getCode(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getFixedExpense() == null ? null : notification.getFixedExpense().getId(),
                notification.getCustomerInquiry() == null ? null : notification.getCustomerInquiry().getId(),
                notification.getCreatedAt()
        );
    }
}
