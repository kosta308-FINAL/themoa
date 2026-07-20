package com.weaone.themoa.domain.notification.support;

import com.weaone.themoa.domain.notification.entity.NotificationType;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.repository.NotificationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 유형 마스터 시드(erd.md §7). {@link NotificationTypeCode}에 값이 있는(=알림을 만드는 기능이 실제로
 * 있는) 코드만 시드한다 — {@code CONNECTION_ERROR}처럼 만드는 기능이 아직 없는 값은 두지 않는다.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class NotificationTypeSeeder implements ApplicationRunner {

    private final NotificationTypeRepository notificationTypeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (notificationTypeRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        notificationTypeRepository.saveAll(List.of(
                NotificationType.seed(NotificationTypeCode.PAYMENT_DUE, "결제 예정", now),
                NotificationType.seed(NotificationTypeCode.MISSED_PAYMENT, "미납", now),
                NotificationType.seed(NotificationTypeCode.AMOUNT_CHANGE, "결제 금액 변경", now),
                NotificationType.seed(NotificationTypeCode.BACKFILL_RECALCULATED, "과거 내역 재계산", now),
                NotificationType.seed(NotificationTypeCode.UNLINKED_CARD_SUSPECTED, "미연동 카드 의심", now),
                NotificationType.seed(NotificationTypeCode.INQUIRY_ANSWERED, "문의 답변 등록", now)
        ));
    }
}
