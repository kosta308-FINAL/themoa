package com.weaone.themoa.domain.subscription.event;

/**
 * 사용자가 예·적금에 새로 가입했다. 고정지출 도메인이 구독해 월납입액을 TRANSFER형 고정지출로
 * 자동 등록한다(fixedExpense.md §7 "저축성 자동이체도 확정 유출로 보아 고정지출에 포함") —
 * subscription 도메인은 fixedexpense를 몰라도 되도록 이벤트로만 알린다(FixedExpenseDetectionBackfillListener와 같은 패턴).
 */
public record SavingsSubscriptionCreatedEvent(Long memberId, Long subscriptionId, String productName,
                                                Long monthlyAmount) {
}
