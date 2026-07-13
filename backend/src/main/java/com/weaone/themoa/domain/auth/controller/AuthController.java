package com.weaone.themoa.domain.auth.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.auth.dto.request.EmailCodeSendRequest;
import com.weaone.themoa.domain.auth.dto.request.EmailCodeVerifyRequest;
import com.weaone.themoa.domain.auth.dto.request.LoginRequest;
import com.weaone.themoa.domain.auth.dto.request.SignupRequest;
import com.weaone.themoa.domain.auth.dto.response.TokenResponse;
import com.weaone.themoa.domain.auth.service.AuthService;
import com.weaone.themoa.domain.auth.service.AuthTokenService;
import com.weaone.themoa.domain.auth.service.EmailVerificationService;
import com.weaone.themoa.domain.auth.service.IssuedTokens;
import com.weaone.themoa.domain.auth.support.RefreshTokenCookieFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService authTokenService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    /** 회원가입 전 이메일 인증 코드 발송. */
    @PostMapping("/email/code")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@Valid @RequestBody EmailCodeSendRequest request) {
        emailVerificationService.sendCode(request.email());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/email/code/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(@Valid @RequestBody EmailCodeVerifyRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signUp(@Valid @RequestBody SignupRequest request) {
        IssuedTokens tokens = authService.signUp(request);
        return tokenResponse(HttpStatus.CREATED, tokens);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        IssuedTokens tokens = authService.login(request);
        return tokenResponse(HttpStatus.OK, tokens);
    }

    /** Refresh rotation. 기존 토큰은 폐기되고 새 토큰 쌍이 나간다. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        IssuedTokens tokens = authTokenService.rotate(refreshToken == null ? "" : refreshToken);
        return tokenResponse(HttpStatus.OK, tokens);
    }

    /** 현재 기기 로그아웃. 다른 기기 세션은 유지된다. 이미 폐기된 토큰이어도 성공으로 처리한다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        authTokenService.revoke(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.expire().toString())
                .build();
    }

    private ResponseEntity<ApiResponse<TokenResponse>> tokenResponse(HttpStatus status, IssuedTokens tokens) {
        ResponseCookie cookie = refreshTokenCookieFactory.create(
                tokens.refreshToken(), tokens.refreshTokenValidity());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(TokenResponse.from(tokens)));
    }
}