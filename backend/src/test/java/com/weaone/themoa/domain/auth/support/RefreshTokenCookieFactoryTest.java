package com.weaone.themoa.domain.auth.support;

import com.weaone.themoa.config.AuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refresh 쿠키의 SameSite 정책 검증(auth.md §8): 일반 발급은 Lax, /refresh·/logout은 Strict.
 */
class RefreshTokenCookieFactoryTest {

    private final RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(
            new AuthProperties(
                    new AuthProperties.Jwt("secret", Duration.ofMinutes(30)),
                    new AuthProperties.Refresh(Duration.ofDays(5), "/api/auth", true),
                    new AuthProperties.EmailVerification(Duration.ofMinutes(5), Duration.ofSeconds(60), 5,
                            Duration.ofMinutes(10), "no-reply@example.com"),
                    new AuthProperties.Terms("2026-07-21")
            ));

    @Test
    @DisplayName("가입·로그인 등 일반 발급 쿠키는 SameSite=Lax다")
    void createUsesLaxSameSite() {
        ResponseCookie cookie = factory.create("token", Duration.ofDays(5));

        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    @DisplayName("/refresh rotation 쿠키는 SameSite=Strict다")
    void createForRefreshUsesStrictSameSite() {
        ResponseCookie cookie = factory.createForRefresh("token", Duration.ofDays(5));

        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    @DisplayName("로그아웃 만료 쿠키는 SameSite=Strict이고 즉시 만료된다")
    void expireUsesStrictSameSiteAndZeroMaxAge() {
        ResponseCookie cookie = factory.expire();

        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
