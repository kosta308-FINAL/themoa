package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.cardtransaction.event.InitialCardBackfillCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 최초 3개월 백필 완료 시 코칭 카드를 1회 즉시 생성한다(habitExpense.md §3). cardtransaction 도메인이
 * 발행한 이벤트를 구독해 역방향 의존을 만들지 않는다({@link
 * com.weaone.themoa.domain.cardtransaction.service.CardConnectionBackfillListener}와 같은 패턴).
 */
@Component
@RequiredArgsConstructor
public class HabitCoachingBackfillListener {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final HabitCoachingCardBatchService habitCoachingCardBatchService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInitialBackfillCompleted(InitialCardBackfillCompletedEvent event) {
        habitCoachingCardBatchService.generateForMember(event.memberId(), LocalDate.now(ZONE_SEOUL));
    }
}
