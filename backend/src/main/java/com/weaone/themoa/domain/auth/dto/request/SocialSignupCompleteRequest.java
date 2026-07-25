package com.weaone.themoa.domain.auth.dto.request;

import com.weaone.themoa.domain.member.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 소셜(카카오·구글) 신규 회원 가입 완료(auth.md §6-2). provider 응답만으로는 이메일·성별·출생일이
 * 없어 추가로 받는다. 비밀번호는 받지 않는다 — 소셜 회원에게는 요구하지 않는다. 닉네임도 받지 않고
 * signupTicket에 담긴 provider 닉네임을 재사용한다(SocialAuthService).
 */
public record SocialSignupCompleteRequest(

        @NotBlank(message = "가입 세션이 유효하지 않습니다.")
        String signupTicket,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @NotNull(message = "성별을 선택해 주세요.")
        Gender gender,

        @NotNull(message = "출생일을 입력해 주세요.")
        @Past(message = "출생일이 올바르지 않습니다.")
        LocalDate birthDate,

        @NotNull(message = "서비스 이용약관에 동의해 주세요.")
        Boolean agreedServiceTerms,

        @NotNull(message = "개인정보 수집·이용에 동의해 주세요.")
        Boolean agreedPrivacyPolicy,

        /** 선택 동의. 서비스 제공에 필수가 아니므로 미동의여도 가입을 막지 않는다. */
        Boolean agreedDataCollection
) {
}
