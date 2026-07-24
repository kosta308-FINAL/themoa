package com.weaone.themoa.domain.fixedexpense.service;

import java.util.List;

/**
 * 고정지출 코칭 카드 대상 선정 + 문구 생성 LLM 계층. 여기서 하는 판단은 딱 하나 — 입력 후보 중 월세·
 * 관리비·보험·대출상환처럼 필수·의무 성격의 지출을 이름·카테고리로 가려내 제외하고, 나머지(구독·여가성 등
 * 재량 조정 가능한 항목) 중 최대 3개만 골라 담담한 "연 환산" 문구를 쓴다. 금액 산수는 하지 않는다 — 입력으로
 * 받은 monthlyAmount·annualAmount를 그대로 서술만 한다. 호출 자체가 실패하면 빈 리스트를 반환해 호출자가
 * 이번 주기는 카드 없음으로 넘어가게 한다(선정 판단을 대신할 안전한 규칙이 없으므로 템플릿으로 전량
 * 대체하지 않는다).
 */
public interface FixedExpenseCoachingLlmClient {

    List<FixedExpenseCoachingDraft> selectAndDraft(List<FixedExpenseCoachingCandidate> candidates);
}
