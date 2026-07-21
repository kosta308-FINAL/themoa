package com.weaone.themoa.domain.auth.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.auth.dto.request.ChangePasswordRequest;
import com.weaone.themoa.domain.auth.dto.request.EmailCodeSendRequest;
import com.weaone.themoa.domain.auth.dto.request.EmailCodeVerifyRequest;
import com.weaone.themoa.domain.auth.dto.request.FindEmailRequest;
import com.weaone.themoa.domain.auth.dto.request.LoginRequest;
import com.weaone.themoa.domain.auth.dto.request.PasswordResetRequest;
import com.weaone.themoa.domain.auth.dto.request.SignupRequest;
import com.weaone.themoa.domain.auth.dto.request.WithdrawRequest;
import com.weaone.themoa.domain.auth.dto.response.FindEmailResponse;
import com.weaone.themoa.domain.auth.dto.response.TokenResponse;
import com.weaone.themoa.domain.auth.service.AuthService;
import com.weaone.themoa.domain.auth.service.AuthTokenService;
import com.weaone.themoa.domain.auth.service.EmailVerificationService;
import com.weaone.themoa.domain.auth.service.IssuedTokens;
import com.weaone.themoa.domain.auth.service.PasswordResetService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final PasswordResetService passwordResetService;
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

    /** 아이디(이메일) 찾기: 닉네임+출생일이 정확히 1건 일치할 때만 마스킹된 이메일을 내려준다. */
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<FindEmailResponse>> findEmail(@Valid @RequestBody FindEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.findEmail(request)));
    }

    /** 비밀번호 찾기 1단계: 가입된 이메일로만 인증 코드를 보낸다(회원가입 코드 발송과 조건이 반대). */
    @PostMapping("/password/reset/code")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetCode(@Valid @RequestBody EmailCodeSendRequest request) {
        passwordResetService.sendCode(request.email());
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 비밀번호 찾기 2단계: 인증 코드 확인. 통과하면 다음 단계(재설정)에서 1회 소비된다. */
    @PostMapping("/password/reset/code/verify")
    public ResponseEntity<ApiResponse<Void>> verifyPasswordResetCode(
            @Valid @RequestBody EmailCodeVerifyRequest request) {
        passwordResetService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 비밀번호 찾기 3단계: 새 비밀번호로 변경. 성공하면 전 세션이 즉시 무효화되어 재로그인이 필요하다. */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
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

    /**
     * 회원 탈퇴(마이페이지). 비밀번호 확인 후 즉시 처리되며 전 세션이 무효화되어 다시 로그인할 수 없다.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody WithdrawRequest request) {
        authService.withdraw(memberId, request);
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