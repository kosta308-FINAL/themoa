package com.weaone.themoa.domain.coaching.service;

/**
 * LLM 구조화 출력 1건(habitExpense.md §4). {@code targetRef}는 규칙 계층이 넘긴
 * {@link HabitCoachingCandidate#targetRef()}를 그대로 되돌려받는 값이다. 절감액은 여기 담지 않는다 —
 * 화면에 쓰는 값은 항상 규칙 계층이 계산한 {@link HabitCoachingCandidate#estimatedSaving()}이다.
 */
public record CoachingCardDraft(String targetRef, String title, String body) {
}
