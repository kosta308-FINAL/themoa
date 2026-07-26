package com.weaone.themoa.domain.subscription.event;

import java.time.LocalDate;

/**
 * 사용자가 예·적금에 새로 가입했다. 고정지출 도메인이 구독해 월납입액을 TRANSFER형 고정지출로
 * 자동 등록한다(fixedExpense.md §7 "저축성 자동이체도 확정 유출로 보아 고정지출에 포함") —
 * subscription 도메인은 fixedexpense를 몰라도 되도록 이벤트로만 알린다(FixedExpenseDetectionBackfillListener와 같은 패턴).
 *
 * @param startDate 가입일. 적금 자동이체일은 통상 가입일과 같은 날짜라 결제일(expectedPayDay) 추정에 쓴다.
 */
public record SavingsSubscriptionCreatedEvent(Long memberId, Long subscriptionId, String productName,
                                                Long monthlyAmount, LocalDate startDate) {
}
