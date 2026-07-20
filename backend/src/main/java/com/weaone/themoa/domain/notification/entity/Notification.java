package com.weaone.themoa.domain.notification.entity;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 앱 내 알림(erd.md §7). 웹 SPA라 네이티브 푸시가 불가능해 목록 단일 채널로만 전달한다.
 * 이동 대상은 {@code fixedExpense} 하나뿐이다 — 카드 연동 도메인의 알림(연결 오류 등)은
 * 아직 이 알림을 만들지 않으므로 {@code card_connection_id} 컬럼을 두지 않는다({@link NotificationType}).
 */
@Entity
@Table(name = "notification",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_dedup", columnNames = {"member_id", "dedup_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_expense_id")
    private FixedExpense fixedExpense;

    /** 같은 알림의 중복 적재를 막는 조립 문자열(예: {@code MISSED_PAYMENT:fe=12:2026-07}). */
    @Column(name = "dedup_key", nullable = false, length = 150)
    private String dedupKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Notification(Member member, NotificationType type, String message, FixedExpense fixedExpense,
                          String dedupKey, LocalDateTime createdAt) {
        this.member = member;
        this.type = type;
        this.message = message;
        this.read = false;
        this.fixedExpense = fixedExpense;
        this.dedupKey = dedupKey;
        this.createdAt = createdAt;
    }

    public static Notification create(Member member, NotificationType type, String message,
                                       FixedExpense fixedExpense, String dedupKey, LocalDateTime createdAt) {
        return new Notification(member, type, message, fixedExpense, dedupKey, createdAt);
    }

    public void markRead(LocalDateTime now) {
        this.read = true;
        this.readAt = now;
    }
}
