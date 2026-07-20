package com.weaone.themoa.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 유형 마스터(erd.md §7). 알림 유형이 도메인마다 계속 늘어날 수 있어 {@code notification.type} ENUM
 * 대신 이 테이블의 FK로 관리한다. {@code code}는 저장 후 바꾸지 않는 불변 식별자이고, 이 코드가 가리키는
 * 알림을 실제로 만드는 호출부의 집합은 {@link NotificationTypeCode}로 애플리케이션에서 관리한다.
 */
@Entity
@Table(name = "notification_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private NotificationType(String code, String name, LocalDateTime now) {
        this.code = code;
        this.name = name;
        this.active = true;
        this.createdAt = now;
    }

    public static NotificationType seed(NotificationTypeCode code, String name, LocalDateTime now) {
        return new NotificationType(code.name(), name, now);
    }
}
