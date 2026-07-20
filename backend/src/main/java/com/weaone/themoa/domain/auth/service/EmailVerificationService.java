package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.support.EmailNormalizer;
import com.weaone.themoa.domain.auth.support.EmailVerificationStore;
import com.weaone.themoa.domain.auth.support.VerificationMailSender;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 회원 생성 전에 이메일 소유를 6자리 코드로 검증한다.
 * 인증을 가입보다 먼저 끝내므로 member는 항상 검증된 이메일로만 만들어진다.
 */
@Service
public class EmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;
    private static final String CODE_FORMAT = "%06d";

    private final MemberRepository memberRepository;
    private final EmailVerificationStore store;
    private final VerificationMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final int maxVerifyAttempts;

    public EmailVerificationService(MemberRepository memberRepository,
                                    EmailVerificationStore store,
                                    VerificationMailSender mailSender,
                                    AuthProperties properties) {
        this.memberRepository = memberRepository;
        this.store = store;
        this.mailSender = mailSender;
        this.maxVerifyAttempts = properties.emailVerification().maxVerifyAttempts();
    }

    public void sendCode(String rawEmail) {
        String email = EmailNormalizer.normalize(rawEmail);
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
        if (store.isInResendCooldown(email)) {
            throw new BusinessException(ErrorCode.AUTH_VERIFICATION_RESEND_COOLDOWN);
        }

        String code = generateCode();
        store.saveCode(email, code);
        try {
            mailSender.send(email, code);
        } catch (BusinessException e) {
            store.invalidateCode(email);
            store.clearResendCooldown(email);
            throw e;
        }
    }

    /** 코드가 맞으면 해당 이메일을 인증 완료 상태로 표시한다. 오입력이 한도에 도달하면 코드를 폐기한다. */
    public void verifyCode(String rawEmail, String code) {
        String email = EmailNormalizer.normalize(rawEmail);
        EmailVerificationStore.VerificationCode saved = store.findCode(email)
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

    /** 회원가입 시 호출. 인증을 통과하지 않은 이메일이면 가입을 막는다. */
    public void requireVerified(String normalizedEmail) {
        if (!store.consumeVerified(normalizedEmail)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
    }

    private String generateCode() {
        return CODE_FORMAT.formatted(random.nextInt(CODE_BOUND));
    }
}