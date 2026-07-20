package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.auth.dto.request.LoginRequest;
import com.weaone.themoa.domain.auth.dto.request.SignupRequest;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "Password1!@#";
    private static final String PASSWORD_HASH = "$2a$10$hashed";
    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1996, 5, 20);

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest(String password, String passwordConfirm, LocalDate birthDate) {
        return new SignupRequest("  User@Example.COM ", password, passwordConfirm, "닉네임", Gender.MALE, birthDate);
    }

    private Member existingMember() {
        return Member.signUp(EMAIL, PASSWORD_HASH, "닉네임", Gender.MALE, ADULT_BIRTH_DATE, LocalDateTime.now());
    }

    private IssuedTokens issuedTokens() {
        return new IssuedTokens("access-token", Duration.ofMinutes(30), "refresh-token", Duration.ofDays(5));
    }

    @Test
    @DisplayName("가입 성공 시 이메일을 정규화해 저장하고 토큰을 발급한다")
    void signUpSuccess() {
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(PASSWORD_HASH);
        given(authTokenService.issue(any(Member.class), any(LocalDateTime.class))).willReturn(issuedTokens());

        IssuedTokens tokens = authService.signUp(signupRequest(RAW_PASSWORD, RAW_PASSWORD, ADULT_BIRTH_DATE));

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        then(emailVerificationService).should().requireVerified(EMAIL);
        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409로 거부하고 저장하지 않는다")
    void signUpRejectsDuplicatedEmail() {
        given(memberRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(signupRequest(RAW_PASSWORD, RAW_PASSWORD, ADULT_BIRTH_DATE)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_EMAIL_DUPLICATED);
        then(memberRepository).should(never()).save(any(Member.class));
    }

    @Test
    @DisplayName("이메일 인증을 통과하지 않으면 회원을 만들지 않는다")
    void signUpRequiresVerifiedEmail() {
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        doThrow(new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED))
                .when(emailVerificationService).requireVerified(EMAIL);

        assertThatThrownBy(() -> authService.signUp(signupRequest(RAW_PASSWORD, RAW_PASSWORD, ADULT_BIRTH_DATE)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        then(memberRepository).should(never()).save(any(Member.class));
    }

    @Test
    @DisplayName("비밀번호 확인이 다르면 400으로 거부한다")
    void signUpRejectsPasswordConfirmMismatch() {
        assertThatThrownBy(() -> authService.signUp(signupRequest(RAW_PASSWORD, "Different1!@#", ADULT_BIRTH_DATE)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
    }

    @Test
    @DisplayName("만 19세 미만은 가입할 수 없다")
    void signUpRejectsUnderage() {
        LocalDate underage = LocalDate.now().minusYears(19).plusDays(1);

        assertThatThrownBy(() -> authService.signUp(signupRequest(RAW_PASSWORD, RAW_PASSWORD, underage)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_UNDERAGE);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰을 발급하고 최종 이용시각을 갱신한다")
    void loginSuccess() {
        Member member = existingMember();
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).willReturn(true);
        given(authTokenService.issue(any(Member.class), any(LocalDateTime.class))).willReturn(issuedTokens());

        IssuedTokens tokens = authService.login(new LoginRequest("USER@example.com ", RAW_PASSWORD));

        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        assertThat(member.getLastActiveAt()).isNotNull();
        then(loginAttemptService).should(never()).recordFailure(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("가입되지 않은 이메일은 비밀번호 오류와 같은 401로 응답한다")
    void loginRejectsUnknownEmail() {
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401로 응답하고 실패 횟수를 기록한다")
    void loginRecordsFailure() {
        Member member = existingMember();
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "WrongPassword1!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        then(loginAttemptService).should().recordFailure(any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("잠긴 계정은 올바른 비밀번호여도 401로 거부한다")
    void loginRejectsLockedAccount() {
        Member member = existingMember();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            member.recordLoginFailure(now);
        }
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        then(authTokenService).should(never()).issue(any(Member.class), any(LocalDateTime.class));
    }
}