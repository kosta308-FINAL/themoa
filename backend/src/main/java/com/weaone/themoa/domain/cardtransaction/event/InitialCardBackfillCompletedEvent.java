package com.weaone.themoa.domain.cardtransaction.event;

/**
 * 신규 커넥션의 최초 3개월 백필이 성공적으로 끝났다(entryMode.md §3). 습관성 지출 코칭(habitExpense.md §3)이
 * "최초 카드 3개월 백필 완료 시 1회 즉시 생성"을 트리거하기 위해 구독한다. 갭 백필(자동수집 재개)에는
 * 발행하지 않는다 — 그건 "최초"가 아니다.
 */
public record InitialCardBackfillCompletedEvent(Long memberId) {
}
