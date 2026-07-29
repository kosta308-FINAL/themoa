package com.weaone.themoa.domain.member.dto;

import jakarta.validation.constraints.Size;

/** 관리자 강제 로그아웃(전 기기) 요청. 사유는 선택이다. */
public record AdminMemberForceLogoutRequest(

        @Size(max = 500, message = "사유는 500자 이내로 입력해 주세요.")
        String reason
) {
}
