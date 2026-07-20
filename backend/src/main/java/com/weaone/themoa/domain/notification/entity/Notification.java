package com.weaone.themoa.domain.notification.entity;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

/**
 * 앱 내 알림(erd.md §7). 웹 SPA라 네이티브 푸시가 불가능해 목록 단일 채널로만 전달한다.
 * 이동 대상은 {@code fixedExpense}·{@code customerInquiry} 둘뿐이다 — 카드 연동 도메인의 알림(연결
 * 오류 등)은 아직 이 알림을 만들지 않으므로 {@code card_connection_id} 컬럼을 두지 않는다.
 * 유형은 ENUM이 아니라 {@link NotificationType} 마스터 FK로 관리한다(erd.md §7 "사실상 재설계").
 */
@Entity
@Table(name = "notification",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_dedup", columnNames = {"member_id", "dedup_key"}))
@Check(constraints = "(case when fixed_expense_id is null then 0 else 1 end)"
        + " + (case when customer_inquiry_id is null then 0 else 1 end) <= 1")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_type_id", nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_expense_id")
    private FixedExpense fixedExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_inquiry_id")
    private CustomerInquiry customerInquiry;

    /** 같은 알림의 중복 적재를 막는 조립 문자열(예: {@code MISSED_PAYMENT:fe=12:2026-07}). */
    @Column(name = "dedup_key", nullable = false, length = 150)
    private String dedupKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Notification(Member member, NotificationType notificationType, String message, FixedExpense fixedExpense,
                          CustomerInquiry customerInquiry, String dedupKey, LocalDateTime createdAt) {
        this.member = member;
        this.notificationType = notificationType;
        this.message = message;
        this.read = false;
        this.fixedExpense = fixedExpense;
        this.customerInquiry = customerInquiry;
        this.dedupKey = dedupKey;
        this.createdAt = createdAt;
    }

    public static Notification create(Member member, NotificationType notificationType, String message,
                                       FixedExpense fixedExpense, CustomerInquiry customerInquiry, String dedupKey,
                                       LocalDateTime createdAt) {
        return new Notification(member, notificationType, message, fixedExpense, customerInquiry, dedupKey,
                createdAt);
    }

    public void markRead(LocalDateTime now) {
        this.read = true;
        this.readAt = now;
    }
}
