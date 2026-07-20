package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardconnection.event.CardConnectionEstablishedEvent;
import com.weaone.themoa.domain.cardconnection.event.CardConnectionRetryRequestedEvent;
import com.weaone.themoa.domain.cardconnection.event.CardSyncResumedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * cardconnection 도메인이 발행한 이벤트를 받아 백필을 실행한다(entryMode.md §3). 커넥션 커밋 후에만
 * 실행하고({@code AFTER_COMMIT}) 별도 스레드로 돌려({@code @Async}) 연결 API 응답을 붙잡지 않는다(§3
 * "백필은 요청을 붙잡지 않고 백그라운드 실행").
 */
@Component
@RequiredArgsConstructor
public class CardConnectionBackfillListener {

    private final CardTransactionBackfillService cardTransactionBackfillService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConnectionEstablished(CardConnectionEstablishedEvent event) {
        cardTransactionBackfillService.runInitialBackfill(event.connectionId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCardSyncResumed(CardSyncResumedEvent event) {
        cardTransactionBackfillService.runGapBackfillForMember(event.memberId(), event.recoveryMode());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRetryRequested(CardConnectionRetryRequestedEvent event) {
        cardTransactionBackfillService.retryInitialBackfill(event.connectionId());
    }
}
