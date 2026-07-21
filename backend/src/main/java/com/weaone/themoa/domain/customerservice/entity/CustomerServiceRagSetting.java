package com.weaone.themoa.domain.customerservice.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_service_rag_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerServiceRagSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "top_k", nullable = false)
    private int topK;

    @Column(name = "minimum_similarity", nullable = false)
    private double minimumSimilarity;

    @Lob
    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Member updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CustomerServiceRagSetting(int topK, double minimumSimilarity, String systemPrompt, Member updatedBy,
                                      LocalDateTime now) {
        this.topK = topK;
        this.minimumSimilarity = minimumSimilarity;
        this.systemPrompt = systemPrompt;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

    public static CustomerServiceRagSetting create(int topK, double minimumSimilarity, String systemPrompt,
                                                   Member updatedBy, LocalDateTime now) {
        return new CustomerServiceRagSetting(topK, minimumSimilarity, systemPrompt, updatedBy, now);
    }

    public void update(int topK, double minimumSimilarity, String systemPrompt, Member updatedBy, LocalDateTime now) {
        this.topK = topK;
        this.minimumSimilarity = minimumSimilarity;
        this.systemPrompt = systemPrompt;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }
}
