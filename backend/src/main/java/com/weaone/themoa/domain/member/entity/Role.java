package com.weaone.themoa.domain.member.entity;

/** 회원 역할(erd.md §1). 고객센터 관리자 API 인가 전용이며 가입 요청으로는 받지 않는다. */
public enum Role {
    USER,
    ADMIN
}
