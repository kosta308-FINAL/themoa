package com.weaone.themoa.domain.member.entity;

/** 관리자의 회원 관리 조치 종류(personalinfo.md — 접근기록 관리). */
public enum MemberAdminActionType {
    VIEW_DETAIL,
    LOCK,
    UNLOCK,
    FORCE_LOGOUT,
    FORCE_WITHDRAW
}
