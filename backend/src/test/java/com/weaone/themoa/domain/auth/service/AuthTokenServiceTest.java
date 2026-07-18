package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.entity.RefreshToken;
import com.weaone.themoa.domain.auth.repository.RefreshTokenRepository;
import com.weaone.themoa.domain.auth.support.RefreshTokenGenerator;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    private static final long MEMBER_ID = 7L;
    private static final Duration REFRESH_VALIDITY = Duration.ofDays(5);

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private MemberRepository memberRepository;

    private final RefreshTokenGenerator generator = new RefreshTokenGenerator();
    private AuthTokenService authTokenService;
    private Member member;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt("ignored", Duration.ofMinutes(30)),
                new AuthProperties.Refresh(REFRESH_VALIDITY, "/api/auth", false),
                new AuthProperties.EmailVerification(Duration.ofMinutes(5), Duration.ofSeconds(60), 5,
                        Duration.ofMinutes(30), "test@example.com")
        );
        authTokenService = new AuthTokenService(
                jwtTokenProvider, refreshTokenRepository, memberRepository, generator, properties);

        member = Member.signUp("user@example.com", "hashed", "닉네임", Gender.MALE, LocalDate.of(1996, 5, 20), LocalDateTime.now());
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
    }

    private RefreshToken storedToken(String rawToken, LocalDateTime expiresAt) {
        return RefreshToken.issue(member, generator.hash(rawToken), LocalDateTime.now(), expiresAt);
    }

    @Test
    @DisplayName("발급 시 Refresh Token은 원문이 아니라 해시로 저장된다")
    void issueStoresHashOnly() {
        given(jwtTokenProvider.createAccessToken(anyLong(), anyInt(), any(Instant.class))).willReturn("access-token");
        given(jwtTokenProvider.getAccessTokenValidity()).willReturn(Duration.ofMinutes(30));
        LocalDateTime now = LocalDateTime.now();

        IssuedTokens tokens = authTokenService.issue(member, now);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getTokenHash())
                .isNotEqualTo(tokens.refreshToken())
                .isEqualTo(generator.hash(tokens.refreshToken()));
        assertThat(saved.getExpiresAt()).isEqualTo(now.plus(REFRESH_VALIDITY));
    }

    @Test
    @DisplayName("재발급하면 기존 토큰을 지우고 새 토큰 쌍을 발급한다")
    void rotateReplacesToken() {
        String oldToken = generator.generate();
        given(refreshTokenRepository.findWithMemberByTokenHash(generator.hash(oldToken)))
                .willReturn(Optional.of(storedToken(oldToken, LocalDateTime.now().plusDays(1))));
        given(refreshTokenRepository.deleteByTokenHash(generator.hash(oldToken))).willReturn(1);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(jwtTokenProvider.createAccessToken(anyLong(), anyInt(), any(Instant.class))).willReturn("access-token");
        given(jwtTokenProvider.getAccessTokenValidity()).willReturn(Duration.ofMinutes(30));

        IssuedTokens tokens = authTokenService.rotate(oldToken);

        assertThat(tokens.refreshToken()).isNotEqualTo(oldToken);
        then(refreshTokenRepository).should().deleteByTokenHash(generator.hash(oldToken));
        then(refreshTokenRepository).should().save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("저장되지 않은 Refresh Token은 401로 거부한다")
    void rotateRejectsUnknownToken() {
        given(refreshTokenRepository.findWithMemberByTokenHash(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authTokenService.rotate(generator.generate()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        then(refreshTokenRepository).should(never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("만료된 Refresh Token은 401로 거부하고 행을 지운다")
    void rotateRejectsExpiredToken() {
        String expired = generator.generate();
        given(refreshTokenRepository.findWithMemberByTokenHash(generator.hash(expired)))
                .willReturn(Optional.of(storedToken(expired, LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> authTokenService.rotate(expired))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        then(refreshTokenRepository).should().deleteByTokenHash(generator.hash(expired));
        then(refreshTokenRepository).should(never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("이미 교체된 토큰(동시 재발급 경합에서 진 요청)은 401로 거부한다")
    void rotateRejectsAlreadyRotatedToken() {
        String raced = generator.generate();
        given(refreshTokenRepository.findWithMemberByTokenHash(generator.hash(raced)))
                .willReturn(Optional.of(storedToken(raced, LocalDateTime.now().plusDays(1))));
        // 다른 요청이 먼저 지워 간 상황: DELETE가 0행을 반환한다.
        given(refreshTokenRepository.deleteByTokenHash(generator.hash(raced))).willReturn(0);

        assertThatThrownBy(() -> authTokenService.rotate(raced))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        then(refreshTokenRepository).should(never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그아웃은 해당 기기의 토큰만 지우고 토큰 버전은 건드리지 않는다")
    void revokeDeletesOnlyCurrentDeviceToken() {
        String token = generator.generate();

        authTokenService.revoke(token);

        then(refreshTokenRepository).should().deleteByTokenHash(generator.hash(token));
        assertThat(member.getTokenVersion()).isZero();
    }

    @Test
    @DisplayName("쿠키가 없는 로그아웃도 성공으로 처리한다")
    void revokeIsIdempotentWithoutToken() {
        authTokenService.revoke(null);
        authTokenService.revoke("");

        then(refreshTokenRepository).should(never()).deleteByTokenHash(anyString());
    }
}