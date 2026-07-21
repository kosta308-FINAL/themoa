package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.dto.request.ChangePasswordRequest;
import com.weaone.themoa.domain.auth.dto.request.LoginRequest;
import com.weaone.themoa.domain.auth.dto.request.SignupRequest;
import com.weaone.themoa.domain.auth.entity.MemberTermsAgreement;
import com.weaone.themoa.domain.auth.entity.TermsType;
import com.weaone.themoa.domain.auth.repository.MemberTermsAgreementRepository;
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
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final EmailVerificationService emailVerificationService;
    private final AuthTokenService authTokenService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    /**
     * 이메일 인증을 마친 사용자만 회원이 된다. 생성 즉시 토큰을 발급해 자동 로그인시킨다.
     * user_policy_profile은 여기서 만들지 않는다(정책 추천 진입 시 지연 생성).
     *
     * <p>필수 약관(서비스 이용약관·개인정보 수집이용) 동의를 가장 먼저 검증한다 — 이메일 등 개인정보를
     * 저장하기 전에 동의부터 확정짓는다(개인정보보호법의 "수집 전 동의" 원칙).
     */
    @Transactional
    public IssuedTokens signUp(SignupRequest request) {
        if (!Boolean.TRUE.equals(request.agreedServiceTerms()) || !Boolean.TRUE.equals(request.agreedPrivacyPolicy())) {
            throw new BusinessException(ErrorCode.AUTH_TERMS_REQUIRED);
        }
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
                request.birthDate(),
                now
        );
        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            // 동시 가입으로 UNIQUE 제약에 걸린 경우. 선조회만으로는 막을 수 없다.
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
        saveTermsAgreements(member, request, now);

        member.recordLoginSuccess(now);
        return authTokenService.issue(member, now);
    }

    /**
     * 필수 2종은 항상 저장하고, 선택 동의(데이터 수집·활용)는 사용자가 체크한 경우에만 행을 만든다.
     * 미체크는 "행이 없다"로 표현하며 별도의 거부 행을 남기지 않는다(erd.md §1).
     */
    private void saveTermsAgreements(Member member, SignupRequest request, LocalDateTime now) {
        String version = authProperties.terms().version();
        memberTermsAgreementRepository.save(MemberTermsAgreement.agree(member, TermsType.SERVICE_TERMS, version, now));
        memberTermsAgreementRepository.save(MemberTermsAgreement.agree(member, TermsType.PRIVACY_POLICY, version, now));
        if (Boolean.TRUE.equals(request.agreedDataCollection())) {
            memberTermsAgreementRepository.save(MemberTermsAgreement.agree(member, TermsType.DATA_COLLECTION, version, now));
        }
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

    /**
     * 비밀번호 변경(auth.md §7-3). 성공하면 이 회원의 Refresh Token을 전부 지우고 token_version을 올려
     * 이 기기를 포함한 전 세션을 즉시 무효화한다 — 변경 직후에는 이 기기도 재로그인해야 한다.
     */
    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        if (member.getPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        authTokenService.revokeAll(member);
    }

    /** 전체 기기 로그아웃(auth.md §7-3). 계정 탈취 의심 등 사용자가 명시적으로 요청할 때 호출한다. */
    @Transactional
    public void logoutAllDevices(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        authTokenService.revokeAll(member);
    }

    private boolean isUnderage(LocalDate birthDate, LocalDate today) {
        return Period.between(birthDate, today).getYears() < MIN_SIGNUP_AGE;
    }
}