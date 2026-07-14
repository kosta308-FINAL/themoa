package com.weaone.themoa.domain.cardconnection.entity;

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
 * 회원-카드사 커넥션. 카드사 로그인 자격증명은 저장하지 않고 CODEF connectedId만 보관한다(connection.md §1).
 */
@Entity
@Table(name = "card_connection", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "organization"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization", nullable = false)
    private CardIssuer cardIssuer;

    @Column(name = "connected_id", nullable = false, length = 100)
    private String connectedId;

    /** 조회+저장 전체 성공 후에만 갱신된다(cardtransaction.md 소관). 이 기능(연결 등록)에서는 항상 NULL로 시작한다. */
    @Column(name = "last_successful_sync_at")
    private LocalDateTime lastSuccessfulSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ConnectionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private CardConnection(Member member, CardIssuer cardIssuer, String connectedId, LocalDateTime createdAt) {
        this.member = member;
        this.cardIssuer = cardIssuer;
        this.connectedId = connectedId;
        this.status = ConnectionStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public static CardConnection connect(Member member, CardIssuer cardIssuer, String connectedId, LocalDateTime now) {
        return new CardConnection(member, cardIssuer, connectedId, now);
    }

    /** ERROR/LOCKED 상태에서 재연결에 성공했을 때 호출한다. 로그인 성공이 곧 본인 증명이다(connection.md §5-1). */
    public void reconnect(String connectedId) {
        this.connectedId = connectedId;
        this.status = ConnectionStatus.ACTIVE;
    }

    /** 카드사 계정 잠금(userError=99). 우리 쿨다운과 달리 시간이 지나도 자동 해제되지 않는다(connection.md §5-2). */
    public void markLocked() {
        this.status = ConnectionStatus.LOCKED;
    }
}
