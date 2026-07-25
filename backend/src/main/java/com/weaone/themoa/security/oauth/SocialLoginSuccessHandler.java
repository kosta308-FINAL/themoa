package com.weaone.themoa.security.oauth;

import com.weaone.themoa.domain.auth.service.OAuthExchangeResult;
import com.weaone.themoa.domain.auth.service.SocialAuthService;
import com.weaone.themoa.domain.auth.support.OAuthLoginExchangeStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 카카오/구글 인가 코드 교환·사용자 조회가 끝난 뒤(SocialOAuth2UserService) 호출된다.
 * 기존 회원 로그인 또는 신규가입 분기 결과를 1회용 교환코드로 바꿔 프론트로 redirect한다 —
 * Access/Refresh Token 원문을 콜백 URL의 query parameter에 싣지 않는다(auth.md §6-2·§8).
 */
@Component
@RequiredArgsConstructor
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialAuthService socialAuthService;
    private final OAuthLoginExchangeStore exchangeStore;
    private final OAuthFrontendRedirectResolver redirectResolver;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        SocialOAuth2User principal = (SocialOAuth2User) authentication.getPrincipal();
        OAuthExchangeResult result = socialAuthService.handleLogin(
                principal.getProvider(), principal.getProviderUserId(), principal.getNickname(),
                principal.getEmail(), LocalDateTime.now());
        String code = exchangeStore.issue(result);
        response.sendRedirect(redirectResolver.resolve("/oauth/callback", "code", code));
    }
}
