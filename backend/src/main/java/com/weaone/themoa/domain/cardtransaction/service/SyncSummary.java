package com.weaone.themoa.domain.cardtransaction.service;

/** 수집 결과 집계. {@code locked}는 같은 회원의 동기화가 이미 진행 중이라 이번 요청은 실행되지 않았다는 뜻이다(§6-1). */
public record SyncSummary(int created, int updated, int skipped, boolean locked) {

    public static SyncSummary empty() {
        return new SyncSummary(0, 0, 0, false);
    }

    public static SyncSummary lockedResult() {
        return new SyncSummary(0, 0, 0, true);
    }

    public SyncSummary plus(SyncSummary other) {
        return new SyncSummary(created + other.created, updated + other.updated,
                skipped + other.skipped, locked || other.locked);
    }
}
