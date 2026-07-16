package com.weaone.themoa.domain.notification.entity;

/**
 * 알림 유형(erd.md §7). 카드사 연결 오류(CONNECTION_ERROR)·백필 재계산(BACKFILL_RECALCULATED)·
 * 미연동 카드 의심(UNLINKED_CARD_SUSPECTED)은 그 알림을 만드는 기능(카드 연동 도메인)이 아직 없어
 * 값을 두지 않는다 — 분기 없는 enum 값은 만들지 않는다(erd.md §7 원칙). 해당 기능 구현 시 추가한다.
 */
public enum NotificationType {
    /** 결제 예정일 리마인더(카드 미연동/이체형, fixedExpense.md §6). */
    PAYMENT_DUE,
    /** 미납 알림(카드 연동, 결제일 윈도우 경과 + 이행 기록 없음). */
    MISSED_PAYMENT,
    /** 구독료 변경 감지(신원·주기는 일치하나 금액이 허용오차를 벗어난 경우). */
    AMOUNT_CHANGE
}
