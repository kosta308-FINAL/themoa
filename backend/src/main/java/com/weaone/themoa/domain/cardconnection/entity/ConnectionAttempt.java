package com.weaone.themoa.domain.cardconnection.entity;

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

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 카드사 로그인 실패(CF-12801) 쿨다운 상태. 카드사 로그인 아이디·비밀번호는 저장하지 않는다(connection.md §5-1).
 */
@Entity
@Table(name = "connection_attempt", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "organization"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConnectionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization", nullable = false)
    private CardIssuer cardIssuer;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;

    private ConnectionAttempt(Member member, CardIssuer cardIssuer) {
        this.member = member;
        this.cardIssuer = cardIssuer;
        this.failCount = 0;
    }

    public static ConnectionAttempt start(Member member, CardIssuer cardIssuer) {
        return new ConnectionAttempt(member, cardIssuer);
    }

    public boolean isCoolingDown(LocalDateTime now) {
        return cooldownUntil != null && cooldownUntil.isAfter(now);
    }

    /** 쿨다운 시간이 지났으면 실패 횟수도 함께 초기화한다({@code Member.releaseLockIfExpired}와 동일 패턴). */
    public void releaseCooldownIfExpired(LocalDateTime now) {
        if (cooldownUntil != null && !cooldownUntil.isAfter(now)) {
            cooldownUntil = null;
            failCount = 0;
        }
    }

    public void recordFailure(LocalDateTime now, int maxFailCount, Duration cooldown) {
        failCount++;
        if (failCount >= maxFailCount) {
            cooldownUntil = now.plus(cooldown);
        }
    }

    /** 로그인 성공 시 호출한다. 성공이 곧 본인 증명이므로 카운트를 리셋한다(connection.md §5-1). */
    public void reset() {
        failCount = 0;
        cooldownUntil = null;
    }
}
