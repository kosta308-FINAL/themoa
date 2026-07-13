package com.weaone.themoa.domain.member.entity;

/**
 * 지출 입력 방식. MANUAL(수기) → CARD(카드 연동) 단방향 전이만 존재한다.
 * 카드 연동 후 "수기로 돌아가기"는 이 값의 역전이가 아니라 {@code cardSyncEnabled} 플래그로 표현한다.
 */
public enum EntryMode {
    MANUAL,
    CARD
}