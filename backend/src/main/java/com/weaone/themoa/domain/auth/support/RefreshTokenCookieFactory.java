package com.weaone.themoa.domain.auth.support;

import com.weaone.themoa.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 쿠키. HttpOnly로 JS 접근을 막고, 경로를 인증 API로 좁혀 다른 요청에 실려 나가지 않게 한다.
 * 별도 CSRF 토큰은 두지 않는다 — {@code SameSite}만으로 cross-site 요청의 쿠키 전송을 막는다(auth.md §8).
 * 일반 발급(가입·로그인)은 {@code Lax}, 앱이 직접 호출하는 {@code /refresh}·{@code /logout}은 더 좁은
 * {@code Strict}를 쓴다 — 최초 로그인은 외부 링크發 최상위 이동으로 들어올 수 있어 {@code Strict}면
 * 쿠키가 안 실릴 수 있지만, 재발급·로그아웃은 프론트가 같은 오리진에서 fetch/XHR로만 호출한다.
 */
@Component
public class RefreshTokenCookieFactory {

    /** 컨트롤러의 {@code @CookieValue}가 컴파일 상수를 요구하므로 쿠키 이름은 여기서 고정한다. */
    public static final String COOKIE_NAME = "refresh_token";

    private static final String SAME_SITE_DEFAULT = "Lax";
    private static final String SAME_SITE_STRICT = "Strict";

    private final String path;
    private final boolean secure;

    public RefreshTokenCookieFactory(AuthProperties properties) {
        AuthProperties.Refresh config = properties.refresh();
        this.path = config.cookiePath();
        this.secure = config.cookieSecure();
    }

    /** 가입·로그인 응답에 싣는 일반 발급 쿠키(SameSite=Lax). */
    public ResponseCookie create(String token, Duration validity) {
        return baseBuilder(SAME_SITE_DEFAULT)
                .value(token)
                .maxAge(validity)
                .build();
    }

    /** {@code /refresh} rotation 응답 전용(SameSite=Strict). */
    public ResponseCookie createForRefresh(String token, Duration validity) {
        return baseBuilder(SAME_SITE_STRICT)
                .value(token)
                .maxAge(validity)
                .build();
    }

    /** 로그아웃 시 즉시 만료시킨다(SameSite=Strict). */
    public ResponseCookie expire() {
        return baseBuilder(SAME_SITE_STRICT)
                .value("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String sameSite) {
        return ResponseCookie.from(COOKIE_NAME)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(sameSite);
    }
}