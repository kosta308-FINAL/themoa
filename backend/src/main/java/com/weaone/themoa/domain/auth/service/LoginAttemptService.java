package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 로그인 실패 횟수 기록. 실패는 곧 예외 발생이고 예외는 호출자의 트랜잭션을 롤백시키므로,
 * 같은 트랜잭션에서 카운트를 올리면 그 증가분까지 함께 사라진다. 별도 트랜잭션으로 커밋한다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long memberId, LocalDateTime now) {
        memberRepository.findById(memberId).ifPresent(member -> {
            member.releaseLockIfExpired(now);
            member.recordLoginFailure(now);
        });
    }
}