package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.subscription.event.SavingsSubscriptionAmountChangedEvent;
import com.weaone.themoa.domain.subscription.event.SavingsSubscriptionCreatedEvent;
import com.weaone.themoa.domain.subscription.event.SavingsSubscriptionDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * subscription 도메인이 발행하는 예·적금 가입 이벤트를 구독해 연동 고정지출(경로 C)을 관리한다
 * (fixedExpense.md §7 "저축성 자동이체도 확정 유출로 보아 고정지출에 포함"). subscription 도메인은
 * fixedexpense를 몰라도 되도록 역방향 의존 없이 이벤트로만 통지받는다({@link FixedExpenseDetectionBackfillListener}와 같은 패턴).
 */
@Component
@RequiredArgsConstructor
public class SavingsSubscriptionFixedExpenseListener {

    private final FixedExpenseRegistrationService registrationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(SavingsSubscriptionCreatedEvent event) {
        registrationService.registerFromSavingsSubscription(
                event.memberId(), event.subscriptionId(), event.productName(), event.monthlyAmount());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAmountChanged(SavingsSubscriptionAmountChangedEvent event) {
        registrationService.updateAmountFromSavingsSubscription(event.subscriptionId(), event.monthlyAmount());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(SavingsSubscriptionDeletedEvent event) {
        registrationService.cancelFromSavingsSubscription(event.subscriptionId());
    }
}
