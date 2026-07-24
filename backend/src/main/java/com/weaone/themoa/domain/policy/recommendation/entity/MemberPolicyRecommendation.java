package com.weaone.themoa.domain.policy.recommendation.entity;

import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_policy_recommendation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_policy_recommendation_member_policy",
                        columnNames = {"member_id", "policy_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPolicyRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false)
    private int score;

    @Column(name = "match_reason", nullable = false, length = 500)
    private String matchReason;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static MemberPolicyRecommendation create(Member member, Policy policy, int score, String matchReason,
                                                    LocalDateTime generatedAt) {
        MemberPolicyRecommendation recommendation = new MemberPolicyRecommendation();
        recommendation.member = member;
        recommendation.policy = policy;
        recommendation.score = score;
        recommendation.matchReason = matchReason;
        recommendation.generatedAt = generatedAt;
        recommendation.createdAt = generatedAt;
        return recommendation;
    }
}
