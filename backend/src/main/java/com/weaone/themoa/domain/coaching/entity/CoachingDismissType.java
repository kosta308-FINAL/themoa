package com.weaone.themoa.domain.coaching.entity;

/** 습관 코칭 카드 넘기기 유형(habitExpense.md §5). */
public enum CoachingDismissType {
    /** 필요한 소비 — 다음 주기 톤다운 힌트로만 쓴다(후보에서 빼지 않음). */
    NOT_WASTE,
    /** 그만 보기 — 다음 주기 후보에서 제외한다. */
    HIDE
}
