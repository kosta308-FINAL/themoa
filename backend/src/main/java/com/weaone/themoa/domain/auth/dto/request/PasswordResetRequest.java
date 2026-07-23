package com.weaone.themoa.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 비밀번호 찾기 최종 단계. 이메일 인증 코드 통과가 선행되어야 한다(PasswordResetService.requireVerified). */
public record PasswordResetRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{10,64}$",
                message = "비밀번호는 공백 없이 10~64자이며 영문·숫자·특수문자를 모두 포함해야 합니다."
        )
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.")
        String newPasswordConfirm
) {
}
