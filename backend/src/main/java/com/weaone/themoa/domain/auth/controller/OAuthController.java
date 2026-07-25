package com.weaone.themoa.domain.auth.controller;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.auth.dto.request.OAuthExchangeRequest;
import com.weaone.themoa.domain.auth.dto.request.SocialSignupCompleteRequest;
import com.weaone.themoa.domain.auth.dto.response.OAuthExchangeResponse;
import com.weaone.themoa.domain.auth.dto.response.TokenResponse;
import com.weaone.themoa.domain.auth.service.IssuedTokens;
import com.weaone.themoa.domain.auth.service.OAuthExchangeResult;
import com.weaone.themoa.domain.auth.service.SocialAuthService;
import com.weaone.themoa.domain.auth.support.OAuthLoginExchangeStore;
import com.weaone.themoa.domain.auth.support.RefreshTokenCookieFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 소셜(카카오·구글) 콜백 이후 프론트가 호출하는 API(auth.md §6·§8). 인가 코드 교환·사용자 조회 자체는
 * spring-boot-starter-oauth2-client가 {@code /api/oauth2/authorization/{registrationId}} →
 * {@code /api/login/oauth2/code/{registrationId}}에서 처리하고(SecurityConfig), 여기서는 그 결과를
 * 1회용 교환코드로 넘겨받아 로그인 완료 또는 신규가입 분기를 응답한다.
 */
@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthLoginExchangeStore exchangeStore;
    private final SocialAuthService socialAuthService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    /** 콜백 redirect 직후 프론트가 1회 호출한다. 코드는 소비 즉시 폐기된다. */
    @PostMapping("/exchange")
    public ResponseEntity<ApiResponse<OAuthExchangeResponse>> exchange(@Valid @RequestBody OAuthExchangeRequest request) {
        OAuthExchangeResult result = exchangeStore.consume(request.code())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_OAUTH_EXCHANGE_CODE_INVALID));

        if (result.requiresSignup()) {
            return ResponseEntity.ok(ApiResponse.success(
                    OAuthExchangeResponse.requiresSignup(result.signupTicket(), result.nickname(), result.email())));
        }

        IssuedTokens tokens = result.tokens();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(
                        tokens.refreshToken(), tokens.refreshTokenValidity()).toString())
                .body(ApiResponse.success(OAuthExchangeResponse.loggedIn(TokenResponse.from(tokens))));
    }

    /** 소셜 신규 회원 가입 완료(이메일 인증·성별·출생일 제출). 성공 시 즉시 자동 로그인. */
    @PostMapping("/complete-signup")
    public ResponseEntity<ApiResponse<TokenResponse>> completeSignup(
            @Valid @RequestBody SocialSignupCompleteRequest request) {
        IssuedTokens tokens = socialAuthService.completeSignup(request, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(
                        tokens.refreshToken(), tokens.refreshTokenValidity()).toString())
                .body(ApiResponse.success(TokenResponse.from(tokens)));
    }
}
