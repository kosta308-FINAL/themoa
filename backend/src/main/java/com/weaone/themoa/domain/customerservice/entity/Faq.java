package com.weaone.themoa.domain.customerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FAQ 질문·Markdown 답변(erd.md §8, customerservice.md §0). 답변은 raw HTML을 허용하지 않는
 * CommonMark 원문으로 저장하고, 도움됨/도움되지 않음 집계는 {@link FaqFeedback}에서 한다.
 */
@Entity
@Table(name = "faq", indexes = @Index(name = "idx_faq_category_active_priority",
        columnList = "faq_category_id, is_active, priority"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faq_category_id", nullable = false)
    private FaqCategory faqCategory;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(name = "answer_markdown", nullable = false, columnDefinition = "TEXT")
    private String answerMarkdown;

    @Column(nullable = false)
    private int priority;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Faq(FaqCategory faqCategory, String question, String answerMarkdown, int priority, LocalDateTime now) {
        this.faqCategory = faqCategory;
        this.question = question;
        this.answerMarkdown = answerMarkdown;
        this.priority = priority;
        this.active = true;
        this.createdAt = now;
    }

    public static Faq seed(FaqCategory faqCategory, String question, String answerMarkdown, int priority,
                            LocalDateTime now) {
        return new Faq(faqCategory, question, answerMarkdown, priority, now);
    }
}
