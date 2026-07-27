package com.weaone.themoa.domain.auth.dto.request;

/** 일반 회원은 현재 비밀번호를 제출하고, 비밀번호가 없는 소셜 전용 회원은 로그인 세션으로 확인한다. */
public record WithdrawRequest(

        String password
) {
}
