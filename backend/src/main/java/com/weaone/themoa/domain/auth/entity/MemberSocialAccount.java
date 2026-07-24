package com.weaone.themoa.domain.auth.entity;

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
 * 소셜 로그인 연결 정보(auth.md §3-3). 일반 계정에 사후 연결하는 기능은 없고, 소셜 회원은 가입 시
 * 이 행을 1개만 가진다. 카카오 액세스/Refresh Token은 여기 저장하지 않는다(1회 조회 후 즉시 폐기).
 */
@Entity
@Table(name = "member_social_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_account_provider_user",
                        columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_social_account_member_provider",
                        columnNames = {"member_id", "provider"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private MemberSocialAccount(Member member, SocialProvider provider, String providerUserId, LocalDateTime now) {
        this.member = member;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.createdAt = now;
    }

    public static MemberSocialAccount link(Member member, SocialProvider provider, String providerUserId,
                                            LocalDateTime now) {
        return new MemberSocialAccount(member, provider, providerUserId, now);
    }
}
