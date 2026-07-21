package com.weaone.themoa.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 회원 탈퇴 확인. 현재 비밀번호로 본인 확인 후 처리한다. */
public record WithdrawRequest(

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
