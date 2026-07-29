package com.weaone.themoa.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자 강제 탈퇴 요청. 되돌릴 수 없는 조치라 사유를 필수로 받는다. */
public record AdminMemberWithdrawRequest(

        @NotBlank(message = "탈퇴 처리 사유를 입력해 주세요.")
        @Size(max = 500, message = "탈퇴 처리 사유는 500자 이내로 입력해 주세요.")
        String reason
) {
}
