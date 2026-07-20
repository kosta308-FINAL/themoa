package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardtransaction.event.InitialCardBackfillCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 최초 3개월 백필 완료 시 고정지출 후보 탐지를 1회 즉시 실행한다. cardtransaction 도메인이 발행한 이벤트를
 * 구독해 역방향 의존을 만들지 않는다({@link
 * com.weaone.themoa.domain.coaching.service.HabitCoachingBackfillListener}와 같은 패턴).
 */
@Component
@RequiredArgsConstructor
public class FixedExpenseDetectionBackfillListener {

    private final FixedExpenseDetectionService fixedExpenseDetectionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInitialBackfillCompleted(InitialCardBackfillCompletedEvent event) {
        fixedExpenseDetectionService.detectForMember(event.memberId());
    }
}
