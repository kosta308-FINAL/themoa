package com.weaone.themoa.domain.cardconnection.event;

/**
 * 신규 카드 커넥션 저장 완료(entryMode.md §3). 최초 3개월 백필 트리거용 — 재연결(reconnect)에는 발행하지
 * 않는다. 커넥션·거래를 직접 다루는 cardtransaction 도메인이 이 이벤트를 구독해 백필을 수행한다
 * (cardconnection → cardtransaction 역방향 의존을 만들지 않기 위한 이벤트 기반 분리).
 */
public record CardConnectionEstablishedEvent(Long connectionId) {
}
