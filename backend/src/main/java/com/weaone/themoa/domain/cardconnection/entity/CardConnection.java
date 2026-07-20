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

    /** 최초 3개월 백필의 영속 상태(entryMode.md §3). 커넥션마다 독립적으로 진행된다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "initial_sync_status", nullable = false, length = 15)
    private InitialSyncStatus initialSyncStatus;

    @Column(name = "initial_sync_started_at")
    private LocalDateTime initialSyncStartedAt;

    @Column(name = "initial_sync_completed_at")
    private LocalDateTime initialSyncCompletedAt;

    /** 실패 상태의 화면 분기용 안정 코드. 자격증명·원문 오류 메시지는 저장하지 않는다. */
    @Column(name = "initial_sync_error_code", length = 100)
    private String initialSyncErrorCode;

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
        this.initialSyncStatus = InitialSyncStatus.NOT_STARTED;
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

    /** CODEF 조회 + 저장 트랜잭션이 전체 성공했을 때만 호출한다(cardtransaction.md §6). 실패 시 앞당기지 않는다. */
    public void markSynced(LocalDateTime now) {
        this.lastSuccessfulSyncAt = now;
    }

    /** 백필 시작(entryMode.md §3). 재시도 시 새 시작시각으로 갱신하고 이전 실패코드·완료시각을 지운다. */
    public void startInitialSync(LocalDateTime now) {
        this.initialSyncStatus = InitialSyncStatus.FETCHING;
        this.initialSyncStartedAt = now;
        this.initialSyncCompletedAt = null;
        this.initialSyncErrorCode = null;
    }

    /** 거래 저장이 끝나고 수기 건 대체(§4) 판정 단계로 넘어갈 때. */
    public void markInitialSyncAnalyzing() {
        this.initialSyncStatus = InitialSyncStatus.ANALYZING;
    }

    public void completeInitialSync(LocalDateTime now) {
        this.initialSyncStatus = InitialSyncStatus.COMPLETED;
        this.initialSyncCompletedAt = now;
    }

    /** 서버 재시작으로 FETCHING/ANALYZING이 오래 남는 경우도 이 경로로 정리한다(erd.md 카드사 커넥션 §비고). */
    public void failInitialSync(String errorCode) {
        this.initialSyncStatus = InitialSyncStatus.FAILED;
        this.initialSyncErrorCode = errorCode;
    }
}
