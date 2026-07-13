package com.weaone.themoa.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailCodeVerifyRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "인증 코드를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자입니다.")
        String code
) {
}