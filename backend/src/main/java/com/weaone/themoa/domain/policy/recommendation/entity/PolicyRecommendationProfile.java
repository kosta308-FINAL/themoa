package com.weaone.themoa.domain.policy.recommendation.entity;

import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
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

@Entity
@Table(
        name = "policy_recommendation_profile",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_recommendation_profile_member",
                        columnNames = "member_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyRecommendationProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "residence_sido", nullable = false, length = 50)
    private String residenceSido;

    @Column(name = "residence_sigungu", length = 50)
    private String residenceSigungu;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private UserEmploymentStatus employmentStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PolicyRecommendationProfile create(Member member, String residenceSido, String residenceSigungu,
                                                     UserEmploymentStatus employmentStatus, LocalDateTime now) {
        PolicyRecommendationProfile profile = new PolicyRecommendationProfile();
        profile.member = member;
        profile.residenceSido = residenceSido;
        profile.residenceSigungu = residenceSigungu;
        profile.employmentStatus = employmentStatus;
        profile.createdAt = now;
        profile.updatedAt = now;
        return profile;
    }

    public void update(String residenceSido, String residenceSigungu, UserEmploymentStatus employmentStatus,
                       LocalDateTime now) {
        this.residenceSido = residenceSido;
        this.residenceSigungu = residenceSigungu;
        this.employmentStatus = employmentStatus;
        this.updatedAt = now;
    }
}
