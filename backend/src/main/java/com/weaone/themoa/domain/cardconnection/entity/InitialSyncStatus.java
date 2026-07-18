package com.weaone.themoa.domain.cardconnection.entity;

/** 최초 3개월 백필의 영속 상태(entryMode.md §3, erd.md `card_connection.initial_sync_status`). */
public enum InitialSyncStatus {
    NOT_STARTED,
    FETCHING,
    ANALYZING,
    COMPLETED,
    FAILED
}
