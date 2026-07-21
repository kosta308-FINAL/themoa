package com.weaone.themoa.domain.auth.dto.request;

import com.weaone.themoa.domain.member.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 개인정보 입력값은 5개(이메일·비밀번호·닉네임·성별·출생일)뿐이다.
 * 월소득·재직형태 등 정책 추천 입력은 해당 기능 진입 시점에 따로 수집한다.
 *
 * <p>여기에 필수 약관 동의(서비스 이용약관·개인정보 수집이용) 2개와 선택 동의(데이터 수집·활용) 1개가
 * 더해진다(erd.md §1 member_terms_agreement). 필수 동의가 없으면 이메일 등 개인정보를 저장하기 전에
 * {@code AUTH_TERMS_REQUIRED}로 거부한다.
 */
public record SignupRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        /*
         * 10~64자, 영문+숫자+특수문자 3종 조합, 공백 불가.
         * 상한 64자는 BCrypt의 72바이트 입력 한계를 넘지 않기 위한 값이다.
         */
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{10,64}$",
                message = "비밀번호는 공백 없이 10~64자이며 영문·숫자·특수문자를 모두 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        String passwordConfirm,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

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