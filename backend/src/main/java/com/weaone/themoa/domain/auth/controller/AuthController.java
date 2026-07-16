package com.weaone.themoa.domain.auth.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.auth.dto.request.ChangePasswordRequest;
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
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PatchMapping;
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
        return tokenResponse(HttpStatus.CREATED, tokens, refreshTokenCookieFactory.create(
                tokens.refreshToken(), tokens.refreshTokenValidity()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        IssuedTokens tokens = authService.login(request);
        return tokenResponse(HttpStatus.OK, tokens, refreshTokenCookieFactory.create(
                tokens.refreshToken(), tokens.refreshTokenValidity()));
    }

    /** Refresh rotation. 기존 토큰은 폐기되고 새 토큰 쌍이 나간다. 프론트가 직접 호출하므로 SameSite=Strict. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        IssuedTokens tokens = authTokenService.rotate(refreshToken == null ? "" : refreshToken);
        return tokenResponse(HttpStatus.OK, tokens, refreshTokenCookieFactory.createForRefresh(
                tokens.refreshToken(), tokens.refreshTokenValidity()));
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

    /** 전체 기기 로그아웃(auth.md §7-3). 이 회원의 모든 기기 세션이 즉시 무효화된다(이 기기 포함). */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        authService.logoutAllDevices(memberId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.expire().toString())
                .build();
    }

    /**
     * 비밀번호 변경(auth.md §7-3). 성공하면 이 기기를 포함한 전 세션이 즉시 무효화되므로 다시
     * 로그인해야 한다.
     */
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(memberId, request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.expire().toString())
                .build();
    }

    private ResponseEntity<ApiResponse<TokenResponse>> tokenResponse(HttpStatus status, IssuedTokens tokens,
                                                                       ResponseCookie cookie) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(TokenResponse.from(tokens)));
    }
}