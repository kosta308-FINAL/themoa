package com.weaone.themoa.domain.cardconnection.service;

import com.weaone.themoa.config.CardConnectionProperties;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionAttempt;
import com.weaone.themoa.domain.cardconnection.repository.ConnectionAttemptRepository;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 카드사 로그인 실패 카운트. 실패는 곧 예외로 이어져 호출자(CardConnectionService) 트랜잭션이 롤백되므로,
 * auth 도메인의 LoginAttemptService와 같은 이유로 별도 트랜잭션에서 커밋한다.
 */
@Service
@RequiredArgsConstructor
public class ConnectionAttemptService {

    private final ConnectionAttemptRepository connectionAttemptRepository;
    private final MemberRepository memberRepository;
    private final CardConnectionProperties cardConnectionProperties;

    /** @return 이번 실패로 쿨다운이 걸렸는지 여부. 호출자 트랜잭션에서 다시 조회하면 별도 트랜잭션 커밋 전 상태를 볼 수 있어 이 값으로 바로 알려준다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(Long memberId, CardIssuer cardIssuer, LocalDateTime now) {
        ConnectionAttempt attempt = connectionAttemptRepository
                .findByMember_IdAndCardIssuer_Organization(memberId, cardIssuer.getOrganization())
                .orElseGet(() -> connectionAttemptRepository.save(
                        ConnectionAttempt.start(memberRepository.getReferenceById(memberId), cardIssuer)));
        attempt.releaseCooldownIfExpired(now);
        attempt.recordFailure(now, cardConnectionProperties.maxLoginFailCount(), cardConnectionProperties.loginFailCooldown());
        return attempt.isCoolingDown(now);
    }

    @Transactional
    public void reset(Long memberId, String organization) {
        connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(memberId, organization)
                .ifPresent(ConnectionAttempt::reset);
    }
}
