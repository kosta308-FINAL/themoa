package com.weaone.themoa.domain.notification.entity;

/**
 * 알림을 만드는 기능이 실제로 존재하는 {@code notification_type.code} 값(erd.md §7). DB의
 * {@link NotificationType}은 마스터 테이블이라 값이 늘어날 수 있지만, 이 enum은 "알림을 실제로 적재하는
 * 호출부가 있는 코드"만 담아 분기 없는 값을 만들지 않는다는 원칙(erd.md §7)을 코드에서도 지킨다.
 */
public enum NotificationTypeCode {
    /** 결제 예정일 리마인더(카드 미연동/이체형, fixedExpense.md §6). */
    PAYMENT_DUE,
    /** 미납 알림(카드 연동, 결제일 윈도우 경과 + 이행 기록 없음). */
    MISSED_PAYMENT,
    /** 구독료 변경 감지(신원·주기는 일치하나 금액이 허용오차를 벗어난 경우). */
    AMOUNT_CHANGE,
    /** 카드 백필로 과거 달 통계가 갱신됐음을 알린다(entryMode.md §3-1). */
    BACKFILL_RECALCULATED,
    /** 대체 짝을 못 찾은 수기 카드 건 발견 — 미연동 카드 의심 신호(entryMode.md §4-2). */
    UNLINKED_CARD_SUSPECTED,
    /** 1:1 문의 최초 답변 등록(customerservice.md §7). */
    INQUIRY_ANSWERED
}
