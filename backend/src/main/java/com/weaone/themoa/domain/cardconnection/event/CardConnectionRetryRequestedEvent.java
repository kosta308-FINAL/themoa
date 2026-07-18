package com.weaone.themoa.domain.cardconnection.event;

/** 초기수집 재시도 요청(dayguide.md §8.1·§8.5). FAILED 상태 커넥션에서만 발행된다. */
public record CardConnectionRetryRequestedEvent(Long connectionId) {
}
