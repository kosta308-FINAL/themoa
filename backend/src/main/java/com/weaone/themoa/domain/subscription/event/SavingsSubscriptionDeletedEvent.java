package com.weaone.themoa.domain.subscription.event;

/** 가입 기록이 삭제됐다. 연동된 고정지출을 해지해 다음 주기부터 예산 차감에서 빠지게 한다. */
public record SavingsSubscriptionDeletedEvent(Long subscriptionId) {
}
