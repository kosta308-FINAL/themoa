package com.weaone.themoa.domain.notification.repository;

import com.weaone.themoa.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {

    Optional<NotificationType> findByCodeAndActiveTrue(String code);
}
