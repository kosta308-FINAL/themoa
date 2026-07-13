package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.auth.dto.request.LoginRequest;
import com.weaone.themoa.domain.auth.dto.request.SignupRequest;
import com.weaone.themoa.domain.auth.support.EmailNormalizer;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** 성년·금융계약능력·청년정책 대상과 맞추기 위한 최소 가입 연령. 자기신고 값이며 본인인증으로 검증하지 않는다. */
    private static final int MIN_SIGNUP_AGE = 19;

    private final MemberRepository memberRepository;
    private final EmailVerificationService emailVerificationService;
    private final AuthTokenService authTokenService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일 인증을 마친 사용자만 회원이 된다. 생성 즉시 토큰을 발급해 자동 로그인시킨다.
     * user_policy_profile은 여기서 만들지 않는다(정책 추천 진입 시 지연 생성).
     */
    @Transactional
    public IssuedTokens signUp(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }

        LocalDateTime now = LocalDateTime.now();
        if (isUnderage(request.birthDate(), now.toLocalDate())) {
            throw new BusinessException(ErrorCode.AUTH_UNDERAGE);
        }

        String email = EmailNormalizer.normalize(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
        emailVerificationService.requireVerified(email);

        Member member = Member.signUp(
                email,
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.gender(),
                request.birthDate()
        );
        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            // 동시 가입으로 UNIQUE 제약에 걸린 경우. 선조회만으로는 막을 수 없다.
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }

        member.recordLoginSuccess(now);
        return authTokenService.issue(member, now);
    }

    /**
     * 이메일 없음과 비밀번호 불일치, 계정 잠금을 모두 같은 401로 응답해 계정 존재 여부를 노출하지 않는다.
     */
    @Transactional
    public IssuedTokens login(LoginRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        LocalDateTime now = LocalDateTime.now();
        member.releaseLockIfExpired(now);
        if (member.isLocked(now)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 소셜 전용 회원은 비밀번호가 없다. 비밀번호 로그인을 시도하면 자격증명 불일치로 처리한다.
        if (member.getPassword() == null
                || !passwordEncoder.matches(request.password(), member.getPassword())) {
            loginAttemptService.recordFailure(member.getId(), now);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        member.recordLoginSuccess(now);
        return authTokenService.issue(member, now);
    }

    private boolean isUnderage(LocalDate birthDate, LocalDate today) {
        return Period.between(birthDate, today).getYears() < MIN_SIGNUP_AGE;
    }
}