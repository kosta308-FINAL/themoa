package com.weaone.themoa.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자 수동 계정 잠금 요청. 감사 로그에 남기기 위해 사유를 필수로 받는다. */
public record AdminMemberLockRequest(

        @NotBlank(message = "잠금 사유를 입력해 주세요.")
        @Size(max = 500, message = "잠금 사유는 500자 이내로 입력해 주세요.")
        String reason
) {
}
