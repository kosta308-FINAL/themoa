package com.weaone.themoa.domain.cardconnection.event;

import com.weaone.themoa.domain.cardtransaction.service.RecoveryMode;

/**
 * 카드 자동수집 재개(entryMode.md §2-1, {@code card_sync_enabled} false→true). 갭 백필(§4-1) 트리거용 —
 * OFF로 끄는 쪽은 별도 백필이 필요 없어 이벤트를 발행하지 않는다. {@code recoveryMode}는 사용자가 지정하지
 * 않으면 {@code null}이며, 리스너가 기존 자동 판단(RECOVER_RECENT)으로 취급한다(dayguide.md §8.1).
 */
public record CardSyncResumedEvent(Long memberId, RecoveryMode recoveryMode) {
}
