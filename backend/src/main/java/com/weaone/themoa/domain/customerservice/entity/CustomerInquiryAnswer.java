package com.weaone.themoa.domain.customerservice.entity;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 관리자 답변(erd.md §8). 문의당 1행({@code inquiry_id} UNIQUE)이며, {@link #version}은 JPA
 * {@code @Version} 낙관적 잠금으로 관리한다 — 서비스가 요청 version과 로드된 현재 version을 먼저
 * 비교해 409를 결정하고, 동시 UPDATE 경합은 JPA가 flush 시점에 자동으로 막는다(customerservice.md §8).
 */
@Entity
@Table(name = "customer_inquiry_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerInquiryAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CustomerInquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Member admin;

    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    private String contentMarkdown;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private CustomerInquiryAnswer(CustomerInquiry inquiry, Member admin, String contentMarkdown, LocalDateTime now) {
        this.inquiry = inquiry;
        this.admin = admin;
        this.contentMarkdown = contentMarkdown;
        this.createdAt = now;
    }

    public static CustomerInquiryAnswer create(CustomerInquiry inquiry, Member admin, String contentMarkdown,
                                                 LocalDateTime now) {
        return new CustomerInquiryAnswer(inquiry, admin, contentMarkdown, now);
    }

    /** 답변 수정. 호출 전 요청 version과 {@link #version}이 같은지 서비스가 먼저 검증해야 한다. */
    public void updateContent(String contentMarkdown, LocalDateTime now) {
        this.contentMarkdown = contentMarkdown;
        this.updatedAt = now;
    }
}
