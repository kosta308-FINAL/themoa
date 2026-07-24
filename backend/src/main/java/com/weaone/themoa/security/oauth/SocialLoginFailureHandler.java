package com.weaone.themoa.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * state 불일치, 사용자의 동의 거부 등 실패 시 로그인 화면으로 되돌리고 에러 배너를 띄운다.
 * 어떤 provider에서 실패했는지는 콜백 경로({@code /api/login/oauth2/code/{registrationId}})의
 * 마지막 path segment로 판별한다.
 */
@Component
public class SocialLoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthFrontendRedirectResolver redirectResolver;

    public SocialLoginFailureHandler(OAuthFrontendRedirectResolver redirectResolver) {
        this.redirectResolver = redirectResolver;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        response.sendRedirect(redirectResolver.resolve("/login", "error", resolveProvider(request)));
    }

    private String resolveProvider(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String lastSegment = uri.substring(uri.lastIndexOf('/') + 1);
        return lastSegment.isBlank() ? "social" : lastSegment;
    }
}
