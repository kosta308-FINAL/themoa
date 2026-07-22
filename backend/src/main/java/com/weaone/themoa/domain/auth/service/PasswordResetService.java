package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.support.EmailNormalizer;
import com.weaone.themoa.domain.auth.support.PasswordResetStore;
import com.weaone.themoa.domain.auth.support.VerificationMailSender;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 비밀번호 찾기(재설정) 전용 이메일 인증. 이미 가입된 이메일만 대상이라는 점에서
 * 회원가입 전 소유 확인용 {@link EmailVerificationService}와 조건이 정반대다.
 */
@Service
public class PasswordResetService {

    private static final int CODE_BOUND = 1_000_000;
    private static final String CODE_FORMAT = "%06d";

    private final MemberRepository memberRepository;
    private final PasswordResetStore store;
    private final VerificationMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final int maxVerifyAttempts;

    public PasswordResetService(MemberRepository memberRepository,
                                 PasswordResetStore store,
                                 VerificationMailSender mailSender,
                                 AuthProperties properties) {
        this.memberRepository = memberRepository;
        this.store = store;
        this.mailSender = mailSender;
        this.maxVerifyAttempts = properties.emailVerification().maxVerifyAttempts();
    }

    public void sendCode(String rawEmail) {
        String email = EmailNormalizer.normalize(rawEmail);
        if (!memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_RESET_MEMBER_NOT_FOUND);
        }
        if (store.isInResendCooldown(email)) {
            throw new BusinessException(ErrorCode.AUTH_VERIFICATION_RESEND_COOLDOWN);
        }

        String code = generateCode();
        store.saveCode(email, code);
        try {
            mailSender.sendPasswordResetCode(email, code);
        } catch (BusinessException e) {
            store.invalidateCode(email);
            store.clearResendCooldown(email);
            throw e;
        }
    }

    public void verifyCode(String rawEmail, String code) {
        String email = EmailNormalizer.normalize(rawEmail);
        PasswordResetStore.VerificationCode saved = store.findCode(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_VERIFICATION_CODE_INVALID));

        if (!saved.matches(code)) {
            if (saved.increaseFailCount() >= maxVerifyAttempts) {
                store.invalidateCode(email);
            }
            throw new BusinessException(ErrorCode.AUTH_VERIFICATION_CODE_INVALID);
        }

        store.invalidateCode(email);
        store.markVerified(email);
    }

    /** 비밀번호 재설정 시 호출. 인증을 통과하지 않은 이메일이면 재설정을 막는다. */
    public void requireVerified(String normalizedEmail) {
        if (!store.consumeVerified(normalizedEmail)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
    }

    private String generateCode() {
        return CODE_FORMAT.formatted(random.nextInt(CODE_BOUND));
    }
}
