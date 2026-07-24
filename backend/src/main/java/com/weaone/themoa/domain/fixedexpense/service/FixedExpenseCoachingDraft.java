package com.weaone.themoa.domain.fixedexpense.service;

/**
 * LLM 구조화 출력 1건. {@code targetRef}는 {@link FixedExpenseCoachingCandidate#targetRef()}를 그대로
 * 되돌려받은 값이다 — 이 목록에 없는 후보는 이번 주기 카드에서 아예 빠진다(=LLM이 필수 지출로 판단해 제외).
 */
public record FixedExpenseCoachingDraft(String targetRef, String title, String body) {
}
