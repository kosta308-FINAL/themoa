package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.support.EmailVerificationStore;
import com.weaone.themoa.domain.auth.support.VerificationMailSender;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String RAW_EMAIL = " User@Example.COM ";
    private static final String EMAIL = "user@example.com";
    private static final int MAX_ATTEMPTS = 5;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private VerificationMailSender mailSender;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt("ignored", Duration.ofMinutes(30)),
                new AuthProperties.Refresh(Duration.ofDays(5), "/api/auth", false),
                new AuthProperties.EmailVerification(Duration.ofMinutes(5), Duration.ofSeconds(60), MAX_ATTEMPTS,
                        Duration.ofMinutes(30), "test@example.com"),
                new AuthProperties.Terms("2026-07-21")
        );
        emailVerificationService = new EmailVerificationService(
                memberRepository, new EmailVerificationStore(properties), mailSender, properties);
    }

    /** 발송된 코드는 응답에 담기지 않으므로 메일 발송 인자에서 가져온다. */
    private String sendAndCaptureCode() {
        emailVerificationService.sendCode(RAW_EMAIL);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        then(mailSender).should().send(anyString(), codeCaptor.capture());
        return codeCaptor.getValue();
    }

    @Test
    @DisplayName("코드 검증에 성공하면 그 이메일로 한 번만 가입할 수 있다")
    void verifiedEmailIsConsumedOnce() {
        String code = sendAndCaptureCode();

        emailVerificationService.verifyCode(RAW_EMAIL, code);

        assertThatCode(() -> emailVerificationService.requireVerified(EMAIL)).doesNotThrowAnyException();
        assertThatThrownBy(() -> emailVerificationService.requireVerified(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("인증하지 않은 이메일로는 가입할 수 없다")
    void requireVerifiedRejectsUnverifiedEmail() {
        assertThatThrownBy(() -> emailVerificationService.requireVerified(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("코드를 5회 틀리면 코드가 폐기되어 올바른 코드도 통과하지 못한다")
    void invalidatesCodeAfterMaxAttempts() {
        String code = sendAndCaptureCode();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> emailVerificationService.verifyCode(RAW_EMAIL, "000000"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.AUTH_VERIFICATION_CODE_INVALID);
        }

        assertThatThrownBy(() -> emailVerificationService.verifyCode(RAW_EMAIL, code))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_VERIFICATION_CODE_INVALID);
    }

    @Test
    @DisplayName("쿨다운 안에 재발송하면 429로 거부한다")
    void rejectsResendWithinCooldown() {
        emailVerificationService.sendCode(RAW_EMAIL);

        assertThatThrownBy(() -> emailVerificationService.sendCode(RAW_EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_VERIFICATION_RESEND_COOLDOWN);
    }

    @Test
    @DisplayName("이미 가입된 이메일에는 코드를 보내지 않는다")
    void rejectsAlreadyRegisteredEmail() {
        given(memberRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendCode(RAW_EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_EMAIL_DUPLICATED);
        then(mailSender).should(never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("메일 발송에 실패하면 쿨다운을 풀어 즉시 다시 시도할 수 있게 한다")
    void clearsCooldownWhenSendFails() {
        willThrow(new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED))
                .given(mailSender).send(anyString(), anyString());

        assertThatThrownBy(() -> emailVerificationService.sendCode(RAW_EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_EMAIL_SEND_FAILED);

        // 쿨다운이 남아 있었다면 429가 났을 것이다.
        assertThatThrownBy(() -> emailVerificationService.sendCode(RAW_EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_EMAIL_SEND_FAILED);
    }
}