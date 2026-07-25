package com.weaone.themoa.domain.auth.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.dto.request.SocialSignupCompleteRequest;
import com.weaone.themoa.domain.auth.entity.MemberSocialAccount;
import com.weaone.themoa.domain.auth.entity.MemberTermsAgreement;
import com.weaone.themoa.domain.auth.entity.SocialProvider;
import com.weaone.themoa.domain.auth.entity.TermsType;
import com.weaone.themoa.domain.auth.repository.MemberSocialAccountRepository;
import com.weaone.themoa.domain.auth.repository.MemberTermsAgreementRepository;
import com.weaone.themoa.domain.auth.support.EmailNormalizer;
import com.weaone.themoa.domain.auth.support.SocialSignupTicketStore;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * 소셜(카카오·구글) 로그인/가입 처리(auth.md §6). 인가 코드 교환·사용자 조회는
 * spring-boot-starter-oauth2-client가 대신하고(SocialOAuth2UserService), 여기서는 그 뒤 커스텀
 * 분기(기존 회원 로그인 vs 신규 가입)만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    /** 일반 가입과 동일한 최소 가입 연령(auth.md §3-2). */
    private static final int MIN_SIGNUP_AGE = 19;

    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final SocialSignupTicketStore signupTicketStore;
    private final EmailVerificationService emailVerificationService;
    private final AuthTokenService authTokenService;
    private final AuthProperties authProperties;

    /**
     * 소셜 콜백 직후 호출한다(SocialLoginSuccessHandler). 기존 연결이 있으면 즉시 로그인 처리하고,
     * 없으면 회원을 만들지 않고 짧은 수명의 가입 티켓만 발급한다(auth.md §6-2, PENDING 상태 없음).
     * @param email 구글처럼 provider가 이미 검증한 이메일이 있을 때만 전달된다(카카오는 항상 null).
     */
    @Transactional
    public OAuthExchangeResult handleLogin(SocialProvider provider, String providerUserId, String nickname,
                                            String email, LocalDateTime now) {
        return memberSocialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(account -> {
                    Member member = account.getMember();
                    member.recordLoginSuccess(now);
                    return OAuthExchangeResult.loggedIn(authTokenService.issue(member, now));
                })
                .orElseGet(() -> OAuthExchangeResult.requiresSignup(
                        signupTicketStore.issue(provider, providerUserId, nickname, email), nickname, email));
    }

    /**
     * 소셜 신규 회원 가입 완료. 필수 약관 동의를 확인한 뒤 member+member_social_account를 한
     * 트랜잭션에서 만들고 즉시 자동 로그인시킨다(auth.md §6-2). 이메일은 ticket에 provider가 이미
     * 검증한 값(구글)이 있으면 그걸 그대로 쓰고 앱 자체 인증은 건너뛴다 — 요청 본문의 email은 그 경우
     * 무시한다(프론트가 읽기 전용으로만 보여줘도, 서버가 신뢰하지 않으므로 위변조해도 의미 없다).
     * 그런 값이 없으면(카카오) 요청의 email로 앱 자체 인증(EmailVerificationService)을 거친다.
     */
    @Transactional
    public IssuedTokens completeSignup(SocialSignupCompleteRequest request, LocalDateTime now) {
        SocialSignupTicketStore.Ticket ticket = signupTicketStore.consume(request.signupTicket())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_OAUTH_SIGNUP_TICKET_INVALID));

        if (!Boolean.TRUE.equals(request.agreedServiceTerms()) || !Boolean.TRUE.equals(request.agreedPrivacyPolicy())) {
            throw new BusinessException(ErrorCode.AUTH_TERMS_REQUIRED);
        }
        if (isUnderage(request.birthDate(), now.toLocalDate())) {
            throw new BusinessException(ErrorCode.AUTH_UNDERAGE);
        }

        String email;
        if (ticket.email() != null && !ticket.email().isBlank()) {
            email = EmailNormalizer.normalize(ticket.email());
        } else {
            email = EmailNormalizer.normalize(request.email());
            emailVerificationService.requireVerified(email);
        }
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }

        String nickname = ticket.nickname() != null && !ticket.nickname().isBlank()
                ? ticket.nickname()
                : "소셜 사용자";

        Member member = Member.signUpSocial(email, nickname, request.gender(), request.birthDate(), now);
        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            // 동시 가입으로 UNIQUE 제약에 걸린 경우. 선조회만으로는 막을 수 없다.
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
        memberSocialAccountRepository.save(
                MemberSocialAccount.link(member, ticket.provider(), ticket.providerUserId(), now));
        saveTermsAgreements(member, request, now);

        member.recordLoginSuccess(now);
        return authTokenService.issue(member, now);
    }

    /** AuthService.saveTermsAgreements와 동일한 규칙(erd.md §1) — 필수 2종은 항상, 선택은 체크 시에만. */
    private void saveTermsAgreements(Member member, SocialSignupCompleteRequest request, LocalDateTime now) {
        String version = authProperties.terms().version();
        memberTermsAgreementRepository.save(MemberTermsAgreement.agree(member, TermsType.SERVICE_TERMS, version, now));
        memberTermsAgreementRepository.save(MemberTermsAgreement.agree(member, TermsType.PRIVACY_POLICY, version, now));
        if (Boolean.TRUE.equals(request.agreedDataCollection())) {
            memberTermsAgreementRepository.save(MemberTermsAgreement.agree(member, TermsType.DATA_COLLECTION, version, now));
        }
    }

    private boolean isUnderage(LocalDate birthDate, LocalDate today) {
        return Period.between(birthDate, today).getYears() < MIN_SIGNUP_AGE;
    }
}
