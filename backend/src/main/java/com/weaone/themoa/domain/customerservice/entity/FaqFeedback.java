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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 회원별 FAQ 도움됨/도움되지 않음 피드백(erd.md §8). 회원당 FAQ 1건에 피드백 1행만 남는다.
 * {@code member_id}는 익명 중복을 막기 위해 NOT NULL이다(customerservice.md §3-3).
 */
@Entity
@Table(name = "faq_feedback",
        uniqueConstraints = @UniqueConstraint(name = "uk_faq_feedback_faq_member", columnNames = {"faq_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faq_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Faq faq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(nullable = false)
    private boolean helpful;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private FaqFeedback(Faq faq, Member member, boolean helpful, LocalDateTime now) {
        this.faq = faq;
        this.member = member;
        this.helpful = helpful;
        this.createdAt = now;
    }

    public static FaqFeedback create(Faq faq, Member member, boolean helpful, LocalDateTime now) {
        return new FaqFeedback(faq, member, helpful, now);
    }

    /** 같은 값 재요청도 멱등하게 허용한다(customerservice.md §4-1). */
    public void changeHelpful(boolean helpful, LocalDateTime now) {
        this.helpful = helpful;
        this.updatedAt = now;
    }
}
