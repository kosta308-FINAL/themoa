package com.weaone.themoa.domain.auth.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token은 원문을 저장하지 않고 SHA-256 해시만 보관한다(DB 유출 시 원문 노출 방지).
 * 회원 1명이 기기별로 여러 행을 가진다(member_id에 UNIQUE를 걸지 않는다).
 * rotation·로그아웃은 행을 삭제한다. 폐기 이력·계보 컬럼은 두지 않는다.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    /** 슬라이딩 만료. 재발급마다 새 행이 생기며 만료 시각이 그때부터 다시 계산된다. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private RefreshToken(Member member, String tokenHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken issue(Member member, String tokenHash, LocalDateTime now, LocalDateTime expiresAt) {
        return new RefreshToken(member, tokenHash, expiresAt, now);
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}