package com.weaone.themoa.domain.subscription.event;

/** 가입 정보 수정으로 월납입액이 바뀌었다. 연동된 고정지출의 예상금액도 같이 갱신하기 위한 이벤트. */
public record SavingsSubscriptionAmountChangedEvent(Long subscriptionId, Long monthlyAmount) {
}
