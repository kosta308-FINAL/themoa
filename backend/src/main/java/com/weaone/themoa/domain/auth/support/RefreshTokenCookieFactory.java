package com.weaone.themoa.domain.auth.support;

import com.weaone.themoa.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 쿠키. HttpOnly로 JS 접근을 막고, 경로를 인증 API로 좁혀 다른 요청에 실려 나가지 않게 한다.
 * SameSite=Strict라 cross-site 요청에는 쿠키가 붙지 않으므로 별도 CSRF 토큰을 두지 않는다.
 */
@Component
public class RefreshTokenCookieFactory {

    private static final String SAME_SITE = "Strict";

    private final String name;
    private final String path;
    private final boolean secure;

    public RefreshTokenCookieFactory(AuthProperties properties) {
        AuthProperties.Refresh config = properties.refresh();
        this.name = config.cookieName();
        this.path = config.cookiePath();
        this.secure = config.cookieSecure();
    }

    public ResponseCookie create(String token, Duration validity) {
        return baseBuilder()
                .value(token)
                .maxAge(validity)
                .build();
    }

    /** 로그아웃 시 즉시 만료시킨다. */
    public ResponseCookie expire() {
        return baseBuilder()
                .value("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder() {
        return ResponseCookie.from(name)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(SAME_SITE);
    }
}