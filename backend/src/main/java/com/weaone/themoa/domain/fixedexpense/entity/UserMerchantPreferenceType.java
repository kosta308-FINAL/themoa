package com.weaone.themoa.domain.fixedexpense.entity;

public enum UserMerchantPreferenceType {
    DO_NOT_RECOMMEND,
    RECLASSIFY_HABIT,
    /** erd.md §4 확정 스키마상 존재하나 MVP 미사용 — 이 값을 저장하는 코드 경로를 만들지 않는다. */
    CATEGORY_OVERRIDE
}
