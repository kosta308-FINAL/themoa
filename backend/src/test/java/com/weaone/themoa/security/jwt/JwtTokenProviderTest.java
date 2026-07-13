package com.weaone.themoa.security.jwt;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(30);

    private JwtTokenProvider provider(String base64Secret) {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt(base64Secret, ACCESS_TOKEN_VALIDITY),
                new AuthProperties.Refresh(Duration.ofDays(5), "refresh_token", "/api/auth", false),
                new AuthProperties.EmailVerification(Duration.ofMinutes(5), Duration.ofSeconds(60), 5,
                        Duration.ofMinutes(30), "test@example.com")
        );
        return new JwtTokenProvider(properties);
    }

    private String secret(String seed) {
        return Base64.getEncoder().encodeToString(seed.getBytes());
    }

    @Test
    @DisplayName("발급한 토큰에서 회원 ID와 토큰 버전을 읽는다")
    void createAndParse() {
        JwtTokenProvider provider = provider(secret("themoa-test-signing-key-32bytes-min!"));

        String token = provider.createAccessToken(42L, 3, Instant.now());

        AccessTokenClaims claims = provider.parse(token);
        assertThat(claims.memberId()).isEqualTo(42L);
        assertThat(claims.tokenVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("만료된 토큰은 401로 거부한다")
    void rejectsExpiredToken() {
        JwtTokenProvider provider = provider(secret("themoa-test-signing-key-32bytes-min!"));
        Instant longAgo = Instant.now().minus(ACCESS_TOKEN_VALIDITY).minusSeconds(60);

        String token = provider.createAccessToken(1L, 0, longAgo);

        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 401로 거부한다")
    void rejectsForgedSignature() {
        String token = provider(secret("themoa-test-signing-key-32bytes-min!"))
                .createAccessToken(1L, 0, Instant.now());
        JwtTokenProvider otherProvider = provider(secret("another-signing-key-that-is-32bytes!!"));

        assertThatThrownBy(() -> otherProvider.parse(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("형식이 아닌 문자열은 401로 거부한다")
    void rejectsMalformedToken() {
        JwtTokenProvider provider = provider(secret("themoa-test-signing-key-32bytes-min!"));

        assertThatThrownBy(() -> provider.parse("not-a-jwt"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("32바이트 미만 서명키는 기동 시점에 거부한다")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> provider(secret("short-key")))
                .isInstanceOf(IllegalStateException.class);
    }
}