package com.weaone.themoa.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 아이디(이메일) 찾기: 가입 시 입력한 닉네임과 출생일이 모두 일치해야 한다. */
public record FindEmailRequest(

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        @NotNull(message = "출생일을 입력해 주세요.")
        @Past(message = "출생일이 올바르지 않습니다.")
        LocalDate birthDate
) {
}
