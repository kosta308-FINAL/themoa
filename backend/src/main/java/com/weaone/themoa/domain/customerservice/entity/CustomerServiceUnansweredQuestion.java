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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 고객센터 챗봇이 근거 부족으로 답을 못했거나 1:1 문의를 안내한 질문 모음(관리자 AI 품질관리 화면에서 조회, erd.md §8). */
@Entity
@Table(name = "customer_service_unanswered_question",
        indexes = @Index(name = "idx_customer_service_unanswered_question_status", columnList = "status, created_at, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerServiceUnansweredQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(nullable = false, length = 500)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UnansweredQuestionReason reason;

    @Lob
    @Column(name = "answer_markdown", columnDefinition = "TEXT")
    private String answerMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnansweredQuestionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    private CustomerServiceUnansweredQuestion(Member member, Long conversationId, String question,
                                              UnansweredQuestionReason reason, String answerMarkdown,
                                              LocalDateTime now) {
        this.member = member;
        this.conversationId = conversationId;
        this.question = question;
        this.reason = reason;
        this.answerMarkdown = answerMarkdown;
        this.status = UnansweredQuestionStatus.NEW;
        this.createdAt = now;
    }

    public static CustomerServiceUnansweredQuestion create(Member member, Long conversationId, String question,
                                                            UnansweredQuestionReason reason, String answerMarkdown,
                                                            LocalDateTime now) {
        return new CustomerServiceUnansweredQuestion(member, conversationId, question, reason, answerMarkdown, now);
    }

    public void updateStatus(UnansweredQuestionStatus status, LocalDateTime now) {
        this.status = status;
        this.resolvedAt = status == UnansweredQuestionStatus.RESOLVED ? now : null;
    }
}
