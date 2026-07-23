package com.weaone.themoa.domain.financialchange.support;

import com.weaone.themoa.domain.notification.entity.NotificationType;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.repository.NotificationTypeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 금융상품 변경 알림 유형 마스터 시드.
 *
 * <p>기존 알림 시더는 "테이블이 완전히 비어 있을 때만" 채우는 구조라, 이미 알림 유형이 들어 있는 DB에는
 * 새 유형이 들어가지 않는다. 그 시더를 고치지 않고도 우리 유형은 확실히 들어가도록 별도로 둔다.
 *
 * <p>이 유형이 없으면 알림 생성 시 "알림 유형 마스터 데이터가 시드되지 않았습니다" 예외가 난다.
 */
@Component
@Order(4)
public class FinancialNotificationTypeSeeder implements ApplicationRunner {

    private final NotificationTypeRepository notificationTypeRepository;

    public FinancialNotificationTypeSeeder(NotificationTypeRepository notificationTypeRepository) {
        this.notificationTypeRepository = notificationTypeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String code = NotificationTypeCode.FINANCIAL_PRODUCT_CHANGED.name();
        if (notificationTypeRepository.findByCodeAndActiveTrue(code).isPresent()) {
            return;
        }
        notificationTypeRepository.save(NotificationType.seed(
                NotificationTypeCode.FINANCIAL_PRODUCT_CHANGED, "관심 상품 변경", LocalDateTime.now()));
    }
}
