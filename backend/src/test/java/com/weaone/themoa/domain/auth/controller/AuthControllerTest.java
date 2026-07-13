package com.weaone.themoa.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.exception.GlobalExceptionHandler;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.dto.request.LoginRequest;
import com.weaone.themoa.domain.auth.dto.request.SignupRequest;
import com.weaone.themoa.domain.auth.service.AuthService;
import com.weaone.themoa.domain.auth.service.AuthTokenService;
import com.weaone.themoa.domain.auth.service.EmailVerificationService;
import com.weaone.themoa.domain.auth.service.IssuedTokens;
import com.weaone.themoa.domain.auth.support.RefreshTokenCookieFactory;
import com.weaone.themoa.domain.member.entity.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 계약 검증. Security 필터를 태우지 않고 컨트롤러와 전역 예외 처리기만 조립한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String VALID_PASSWORD = "Password1!@#";

    @Mock
    private AuthService authService;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private EmailVerificationService emailVerificationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt("ignored", Duration.ofMinutes(30)),
                new AuthProperties.Refresh(Duration.ofDays(5), "/api/auth", false),
                new AuthProperties.EmailVerification(Duration.ofMinutes(5), Duration.ofSeconds(60), 5,
                        Duration.ofMinutes(30), "test@example.com")
        );
        AuthController controller = new AuthController(
                authService, authTokenService, emailVerificationService,
                new RefreshTokenCookieFactory(properties));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private IssuedTokens issuedTokens() {
        return new IssuedTokens("access-token", Duration.ofMinutes(30), "refresh-token", Duration.ofDays(5));
    }

    private String signupBody() throws Exception {
        return objectMapper.writeValueAsString(new SignupRequest(
                "user@example.com", VALID_PASSWORD, VALID_PASSWORD, "닉네임", Gender.MALE, LocalDate.of(1996, 5, 20)));
    }

    @Test
    @DisplayName("가입 성공은 201과 Access Token, HttpOnly Refresh 쿠키를 반환한다")
    void signUpReturnsCreated() throws Exception {
        given(authService.signUp(any(SignupRequest.class))).willReturn(issuedTokens());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().value("refresh_token", "refresh-token"))
                // Refresh Token 원문은 쿠키로만 나가고 응답 본문에는 없어야 한다.
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("\"refreshToken\""))));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409와 AUTH_EMAIL_DUPLICATED 코드를 반환한다")
    void signUpReturnsConflictOnDuplicatedEmail() throws Exception {
        willThrow(new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED))
                .given(authService).signUp(any(SignupRequest.class));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_EMAIL_DUPLICATED"));
    }

    @Test
    @DisplayName("비밀번호 형식이 규칙에 맞지 않으면 400을 반환한다")
    void signUpRejectsWeakPassword() throws Exception {
        String body = objectMapper.writeValueAsString(new SignupRequest(
                "user@example.com", "onlyletters", "onlyletters", "닉네임", Gender.MALE, LocalDate.of(1996, 5, 20)));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("로그인 성공은 200과 토큰을 반환한다")
    void loginReturnsOk() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(issuedTokens());
        String body = objectMapper.writeValueAsString(new LoginRequest("user@example.com", VALID_PASSWORD));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("자격증명이 틀리면 401과 AUTH_INVALID_CREDENTIALS 코드를 반환한다")
    void loginReturnsUnauthorized() throws Exception {
        willThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS))
                .given(authService).login(any(LoginRequest.class));
        String body = objectMapper.writeValueAsString(new LoginRequest("user@example.com", VALID_PASSWORD));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("재발급은 쿠키의 Refresh Token으로 처리하고 새 쿠키를 다시 심는다")
    void refreshRotatesToken() throws Exception {
        given(authTokenService.rotate("old-refresh")).willReturn(issuedTokens());

        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(cookie().value("refresh_token", "refresh-token"));
    }

    @Test
    @DisplayName("로그아웃은 204와 만료된 쿠키를 반환한다")
    void logoutExpiresCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new jakarta.servlet.http.Cookie("refresh_token", "token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("쿠키 없이 로그아웃해도 204로 성공한다")
    void logoutWithoutCookieIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }
}