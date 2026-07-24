package com.weaone.themoa.security.oauth;

import com.weaone.themoa.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/**
 * OAuth2 인가 요청(state 등)을 서버 세션이 아니라 쿠키에 담는다. 이 프로젝트의 인증은 완전히
 * stateless가 원칙이라(auth.md §2-2), 카카오 로그인 핸드셰이크 동안만 필요한 값도 HttpSession에
 * 두지 않는다. 최상위 리다이렉션(카카오 → 콜백)을 타야 해서 SameSite=Lax를 쓴다 — 로그인 자체가
 * 그런 흐름이라는 점에서 RefreshTokenCookieFactory의 일반 발급 쿠키와 같은 근거다.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final String COOKIE_PATH = "/api";
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    private final boolean cookieSecure;

    public HttpCookieOAuth2AuthorizationRequestRepository(AuthProperties properties) {
        this.cookieSecure = properties.refresh().cookieSecure();
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return findCookie(request).map(this::deserialize).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response);
            return;
        }
        addCookie(response, serialize(authorizationRequest));
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                   HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(response);
        return authorizationRequest;
    }

    private java.util.Optional<Cookie> findCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        return java.util.Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .findFirst();
    }

    private void addCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath(COOKIE_PATH);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath(COOKIE_PATH);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(authorizationRequest));
    }

    private OAuth2AuthorizationRequest deserialize(Cookie cookie) {
        Object value = SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.getValue()));
        return (OAuth2AuthorizationRequest) value;
    }
}
