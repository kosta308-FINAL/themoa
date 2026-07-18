package com.weaone.themoa.domain.cardconnection.event;

/**
 * 카드 자동수집 재개(entryMode.md §2-1, {@code card_sync_enabled} false→true). 갭 백필(§4-1) 트리거용 —
 * OFF로 끄는 쪽은 별도 백필이 필요 없어 이벤트를 발행하지 않는다.
 */
public record CardSyncResumedEvent(Long memberId) {
}
