package com.weaone.themoa.domain.auth.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.weaone.themoa.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 이메일 인증 코드·쿨다운·인증완료 상태를 TTL 캐시에 보관한다.
 * 코드와 인증 상태는 회원 생성 전에만 필요한 값이라 DB(member)에 남기지 않는다.
 */
@Component
public class EmailVerificationStore {

    private final Cache<String, VerificationCode> codes;
    private final Cache<String, Boolean> resendCooldowns;
    private final Cache<String, Boolean> verifiedEmails;

    public EmailVerificationStore(AuthProperties properties) {
        AuthProperties.EmailVerification config = properties.emailVerification();
        this.codes = Caffeine.newBuilder().expireAfterWrite(config.codeTtl()).build();
        this.resendCooldowns = Caffeine.newBuilder().expireAfterWrite(config.resendCooldown()).build();
        this.verifiedEmails = Caffeine.newBuilder().expireAfterWrite(config.verifiedTtl()).build();
    }

    public boolean isInResendCooldown(String email) {
        return resendCooldowns.getIfPresent(email) != null;
    }

    public void saveCode(String email, String code) {
        codes.put(email, new VerificationCode(code));
        resendCooldowns.put(email, Boolean.TRUE);
    }

    public Optional<VerificationCode> findCode(String email) {
        return Optional.ofNullable(codes.getIfPresent(email));
    }

    public void invalidateCode(String email) {
        codes.invalidate(email);
    }

    /** 발송 실패 시 쿨다운까지 되돌려 사용자가 즉시 재시도할 수 있게 한다. */
    public void clearResendCooldown(String email) {
        resendCooldowns.invalidate(email);
    }

    public void markVerified(String email) {
        verifiedEmails.put(email, Boolean.TRUE);
    }

    /** 인증 상태는 회원가입 1회에만 쓰이고 소비된다. */
    public boolean consumeVerified(String email) {
        boolean verified = verifiedEmails.getIfPresent(email) != null;
        if (verified) {
            verifiedEmails.invalidate(email);
        }
        return verified;
    }

    /**
     * 코드와 오입력 횟수. 인증 코드는 로그·예외 메시지에 노출하지 않는다.
     */
    public static final class VerificationCode {

        private final String code;
        private final AtomicInteger failCount = new AtomicInteger();

        private VerificationCode(String code) {
            this.code = code;
        }

        public boolean matches(String candidate) {
            return code.equals(candidate);
        }

        public int increaseFailCount() {
            return failCount.incrementAndGet();
        }
    }
}