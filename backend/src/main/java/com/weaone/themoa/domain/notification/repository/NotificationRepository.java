package com.weaone.themoa.domain.notification.repository;

import com.weaone.themoa.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByMember_IdAndDedupKey(Long memberId, String dedupKey);
}
