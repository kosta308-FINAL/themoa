package com.weaone.themoa.domain.notification.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.notification.dto.response.NotificationListResponse;
import com.weaone.themoa.domain.notification.dto.response.NotificationResponse;
import com.weaone.themoa.domain.notification.entity.Notification;
import com.weaone.themoa.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 앱 내 알림 목록 조회·읽음 처리(알림.md MOA-S-NOT-APP-02·-04·-05). 알림 적재(쓰기)는
 * {@link NotificationService} 소관이라 여기서는 조회·상태 변경만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse list(Long memberId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByMember_IdOrderByCreatedAtDesc(memberId, pageable)
                .map(NotificationResponse::from);
        long unreadCount = notificationRepository.countByMember_IdAndReadFalse(memberId);
        return NotificationListResponse.from(page, unreadCount);
    }

    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndMember_Id(notificationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead(LocalDateTime.now());
    }
}
