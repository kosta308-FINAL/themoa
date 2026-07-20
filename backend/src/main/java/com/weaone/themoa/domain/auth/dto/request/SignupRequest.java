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
 * 가입 시 수집하는 값은 5개(이메일·비밀번호·닉네임·성별·출생일)뿐이다.
 * 월소득·재직형태 등 정책 추천 입력은 해당 기능 진입 시점에 따로 수집한다.
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
        LocalDate birthDate
) {
}