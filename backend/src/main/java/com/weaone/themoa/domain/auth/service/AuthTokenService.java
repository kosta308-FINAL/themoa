package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.entity.RefreshToken;
import com.weaone.themoa.domain.auth.repository.RefreshTokenRepository;
import com.weaone.themoa.domain.auth.support.RefreshTokenGenerator;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.security.jwt.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Access/Refresh Token 발급, rotation, 폐기를 담당한다.
 */
@Service
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final Duration refreshTokenValidity;

    public AuthTokenService(JwtTokenProvider jwtTokenProvider,
                            RefreshTokenRepository refreshTokenRepository,
                            MemberRepository memberRepository,
                            RefreshTokenGenerator refreshTokenGenerator,
                            AuthProperties properties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberRepository = memberRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenValidity = properties.refresh().validity();
    }

    /** 로그인·회원가입 직후 호출한다. 호출자의 트랜잭션에 참여한다. */
    @Transactional
    public IssuedTokens issue(Member member, LocalDateTime now) {
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getTokenVersion(),
                now.atZone(ZoneId.systemDefault()).toInstant()
        );
        String refreshToken = refreshTokenGenerator.generate();
        refreshTokenRepository.save(RefreshToken.issue(
                member,
                refreshTokenGenerator.hash(refreshToken),
                now,
                now.plus(refreshTokenValidity)
        ));
        return new IssuedTokens(accessToken, jwtTokenProvider.getAccessTokenValidity(),
                refreshToken, refreshTokenValidity);
    }

    /**
     * 기존 Refresh Token을 폐기하고 새 토큰 쌍을 발급한다.
     * 같은 토큰으로 동시에 들어온 요청은 DELETE 결과 행 수로 걸러 하나만 성공시킨다.
     */
    @Transactional
    public IssuedTokens rotate(String refreshToken) {
        String tokenHash = refreshTokenGenerator.hash(refreshToken);
        RefreshToken saved = refreshTokenRepository.findWithMemberByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN));

        LocalDateTime now = LocalDateTime.now();
        Long memberId = saved.getMember().getId();

        if (saved.isExpired(now)) {
            refreshTokenRepository.deleteByTokenHash(tokenHash);
            throw new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }
        if (refreshTokenRepository.deleteByTokenHash(tokenHash) == 0) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN));
        return issue(member, now);
    }

    /**
     * 현재 기기 로그아웃. 그 기기의 Refresh Token 행만 지우고 token_version은 건드리지 않는다.
     * (올리면 같은 회원의 다른 기기까지 로그아웃된다.) 이미 없는 토큰이어도 성공으로 처리한다.
     */
    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByTokenHash(refreshTokenGenerator.hash(refreshToken));
    }

    /**
     * 전체 기기 로그아웃·비밀번호 변경 전용(auth.md §7-3). 이 회원의 Refresh Token 행을 전부 지우고
     * token_version을 올려 이미 발급된 Access Token까지 즉시 무효화한다.
     */
    @Transactional
    public void revokeAll(Member member) {
        refreshTokenRepository.deleteAllByMemberId(member.getId());
        member.increaseTokenVersion();
    }
}