package com.weaone.themoa.domain.coaching.service;

import java.util.List;

/**
 * 습관 코칭 카드 문구 생성 LLM 계층(habitExpense.md §4). 금액 산수는 절대 하지 않는다 — 규칙 계층이 이미
 * 계산한 값을 문장으로 서술만 한다. 실패(호출 자체 실패)하면 빈 리스트를 반환해 호출자가 전량 템플릿으로
 * 폴백하게 한다.
 */
public interface HabitCoachingLlmClient {

    List<CoachingCardDraft> generateDrafts(List<HabitCoachingCandidate> candidates);
}
