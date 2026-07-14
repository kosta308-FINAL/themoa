package com.weaone.themoa.domain.cardtransaction.service;

/** 승인내역 1건 처리 결과. {@code SKIPPED}는 환율 캐시가 완전히 비어 이 건만 건너뛴 경우다(§4). */
public enum CollectionOutcome {
    CREATED,
    UPDATED,
    SKIPPED
}
