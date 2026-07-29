package com.weaone.themoa.domain.member.dto;

import jakarta.validation.constraints.Size;

/** 관리자 수동 계정 잠금 해제 요청. 사유는 선택이다(잠금 해제는 되돌릴 수 있는 조치라 필수로 강제하지 않는다). */
public record AdminMemberUnlockRequest(

        @Size(max = 500, message = "사유는 500자 이내로 입력해 주세요.")
        String reason
) {
}
