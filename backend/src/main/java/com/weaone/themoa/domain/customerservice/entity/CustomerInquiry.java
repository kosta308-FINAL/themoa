package com.weaone.themoa.domain.customerservice.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 1:1 문의(erd.md §8). 이메일 답변을 하지 않아 {@code contact_email}은 두지 않고, 개인정보 동의는
 * "실제 동의했다"는 증거로 시각·정책 버전을 저장한다(default true는 증거가 되지 않는다).
 *
 * <p>{@code UNIQUE(id, member_id)}는 §7 {@code notification}이 알림 회원과 문의 작성자가 같음을 묶기
 * 위한 인덱스다(erd.md §7). 실제 복합 FK는 Hibernate ddl-auto로 생성하기 어려워 두지 않고, 알림 생성
 * 경로({@code NotificationService})가 항상 문의의 회원을 그대로 사용하는 것으로 애플리케이션이 보장한다.
 */
@Entity
@Table(name = "customer_inquiry",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_inquiry_id_member", columnNames = {"id", "member_id"}),
        indexes = {
                @Index(name = "idx_customer_inquiry_member", columnList = "member_id, created_at, id"),
                @Index(name = "idx_customer_inquiry_status", columnList = "status, inquiry_category_id, created_at, id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_category_id", nullable = false)
    private CustomerInquiryCategory inquiryCategory;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InquiryStatus status;

    @Column(name = "privacy_agreed_at", nullable = false)
    private LocalDateTime privacyAgreedAt;

    @Column(name = "privacy_policy_version", nullable = false, length = 30)
    private String privacyPolicyVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private CustomerInquiry(Member member, CustomerInquiryCategory inquiryCategory, String title, String content,
                             String privacyPolicyVersion, LocalDateTime now) {
        this.member = member;
        this.inquiryCategory = inquiryCategory;
        this.title = title;
        this.content = content;
        this.status = InquiryStatus.PENDING;
        this.privacyAgreedAt = now;
        this.privacyPolicyVersion = privacyPolicyVersion;
        this.createdAt = now;
    }

    public static CustomerInquiry create(Member member, CustomerInquiryCategory inquiryCategory, String title,
                                          String content, String privacyPolicyVersion, LocalDateTime now) {
        return new CustomerInquiry(member, inquiryCategory, title, content, privacyPolicyVersion, now);
    }

    /** 최초 답변 등록 시 호출한다(customerservice.md §7). */
    public void markAnswered(LocalDateTime now) {
        this.status = InquiryStatus.ANSWERED;
        this.updatedAt = now;
    }
}
