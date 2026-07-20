package com.weaone.themoa.domain.notification.service;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.notification.entity.Notification;
import com.weaone.themoa.domain.notification.entity.NotificationType;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.repository.NotificationRepository;
import com.weaone.themoa.domain.notification.repository.NotificationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 알림 적재의 단일 진입점. {@code dedup_key}로 같은 알림을 두 번 쌓지 않는다(erd.md §7).
 * 알림 목록 조회·읽음 처리는 {@link NotificationQueryService} 소관이라 여기는 "쓰기"만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    /** 이미 같은 dedupKey로 쌓인 알림이 있으면 조용히 넘어간다(멱등, 새벽 배치 재실행 대비). */
    @Transactional
    public void createIfAbsent(Member member, NotificationTypeCode typeCode, String message,
                                FixedExpense fixedExpense, CustomerInquiry customerInquiry, String dedupKey) {
        if (notificationRepository.existsByMember_IdAndDedupKey(member.getId(), dedupKey)) {
            return;
        }
        NotificationType notificationType = notificationTypeRepository.findByCodeAndActiveTrue(typeCode.name())
                .orElseThrow(() -> new IllegalStateException("알림 유형 마스터 데이터가 시드되지 않았습니다: " + typeCode));
        try {
            notificationRepository.save(Notification.create(member, notificationType, message, fixedExpense,
                    customerInquiry, dedupKey, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            // 동시 배치 경합으로 이미 다른 스레드가 같은 dedupKey를 저장했다. 무시한다.
        }
    }
}
