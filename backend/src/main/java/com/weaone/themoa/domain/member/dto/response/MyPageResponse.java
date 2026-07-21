package com.weaone.themoa.domain.member.dto.response;

import com.weaone.themoa.domain.auth.entity.MemberTermsAgreement;
import com.weaone.themoa.domain.member.entity.Member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이페이지 요약. 카드 연동 현황은 {@code /api/card-connections}, 저축목표 수정은
 * {@code /api/spending-guide/savings-goal}을 그대로 쓴다 — 여기서는 조회만 담당한다.
 */
public record MyPageResponse(
        ProfileResponse profile,
        BigDecimal savingsTargetAmount,
        List<TermsAgreementResponse> termsAgreements) {

    public record ProfileResponse(
            String email,
            String name,
            String gender,
            LocalDate birthDate,
            String incomeType,
            BigDecimal salaryAmount,
            BigDecimal hourlyWage,
            Integer payday,
            String entryMode,
            boolean cardSyncEnabled,
            LocalDateTime createdAt) {

        public static ProfileResponse from(Member member) {
            return new ProfileResponse(
                    member.getEmail(),
                    member.getName(),
                    member.getGender().name(),
                    member.getBirthDate(),
                    member.getIncomeType().name(),
                    member.getSalaryAmount(),
                    member.getHourlyWage(),
                    member.getPayday(),
                    member.getEntryMode().name(),
                    member.isCardSyncEnabled(),
                    member.getCreatedAt());
        }
    }

    /** 약관 동의 이력 1건(erd.md §1). 행 존재 자체가 동의 증빙이라 별도 boolean 플래그가 없다. */
    public record TermsAgreementResponse(
            String termsType,
            String termsVersion,
            LocalDateTime agreedAt) {

        public static TermsAgreementResponse from(MemberTermsAgreement agreement) {
            return new TermsAgreementResponse(
                    agreement.getTermsType().name(),
                    agreement.getTermsVersion(),
                    agreement.getAgreedAt());
        }
    }

    public static MyPageResponse of(Member member, List<MemberTermsAgreement> termsAgreements) {
        return new MyPageResponse(
                ProfileResponse.from(member),
                member.getSavingsTargetOrZero(),
                termsAgreements.stream().map(TermsAgreementResponse::from).toList());
    }
}
