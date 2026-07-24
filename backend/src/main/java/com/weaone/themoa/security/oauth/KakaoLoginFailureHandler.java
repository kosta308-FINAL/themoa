package com.weaone.themoa.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** state 불일치, 사용자의 카카오 동의 거부 등 실패 시 로그인 화면으로 되돌리고 에러 배너를 띄운다. */
@Component
@RequiredArgsConstructor
public class KakaoLoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthFrontendRedirectResolver redirectResolver;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        response.sendRedirect(redirectResolver.resolve("/login", "error", "kakao"));
    }
}
