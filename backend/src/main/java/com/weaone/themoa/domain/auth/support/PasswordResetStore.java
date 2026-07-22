package com.weaone.themoa.domain.auth.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.weaone.themoa.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 비밀번호 찾기(재설정) 인증 코드·쿨다운·인증완료 상태를 TTL 캐시에 보관한다.
 * 회원가입용 {@link EmailVerificationStore}와 목적이 달라(가입 전 이메일 소유 확인 vs 기존 회원 본인 확인)
 * 별도 인스턴스로 분리했다 — 같은 이메일이라도 두 캐시는 서로 섞이지 않는다.
 */
@Component
public class PasswordResetStore {

    private final Cache<String, VerificationCode> codes;
    private final Cache<String, Boolean> resendCooldowns;
    private final Cache<String, Boolean> verifiedEmails;

    public PasswordResetStore(AuthProperties properties) {
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

    public void clearResendCooldown(String email) {
        resendCooldowns.invalidate(email);
    }

    public void markVerified(String email) {
        verifiedEmails.put(email, Boolean.TRUE);
    }

    /** 인증 상태는 비밀번호 재설정 1회에만 쓰이고 소비된다. */
    public boolean consumeVerified(String email) {
        boolean verified = verifiedEmails.getIfPresent(email) != null;
        if (verified) {
            verifiedEmails.invalidate(email);
        }
        return verified;
    }

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
