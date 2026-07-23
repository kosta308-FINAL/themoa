package com.weaone.themoa.domain.financialchange.repository;

import com.weaone.themoa.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 알림 id로 dedupKey를 찾기 위한 전용 레포지토리.
 *
 * <p>알림 엔티티는 알림 도메인(팀원) 소유라 그쪽 레포지토리에 메서드를 추가하지 않고, 여기서 필요한
 * 조회만 따로 정의한다(다른 도메인 엔티티를 읽을 때 이 프로젝트에서 써온 방식).
 */
public interface FinancialChangeNotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndMember_Id(Long id, Long memberId);
}
