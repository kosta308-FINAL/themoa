package com.weaone.themoa.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 비밀번호 변경(auth.md §7-3). 성공 시 이 회원의 전 기기 세션이 즉시 무효화된다. */
public record ChangePasswordRequest(

        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword,

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
