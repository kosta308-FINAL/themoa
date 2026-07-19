package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.domain.cardtransaction.event.InitialCardBackfillCompletedEvent;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 최초 3개월 백필 완료 시 과거 급여주기 budget row를 소급 생성한다 — 최초 사용자가 가입 직후에도 "이전
 * 주기 조회"를 쓸 수 있게 한다({@link BudgetCycleService#backfillPastCycles}). cardtransaction 도메인이
 * 발행한 이벤트를 구독해 역방향 의존을 만들지 않는다({@link
 * com.weaone.themoa.domain.cardtransaction.service.CardConnectionBackfillListener}와 같은 패턴).
 *
 * <p>이 시점에 소비 가이드 최초 설정(월급·급여일)이 아직 안 끝나 {@code member.payday}가 없으면 소급 생성을
 * 건너뛴다 — 급여주기를 계산할 기준이 없기 때문이다. 이 경우엔 {@link
 * com.weaone.themoa.domain.budget.service.SpendingGuideService#setup}이 완료되는 시점에 같은 소급 생성을
 * 다시 시도한다.
 */
@Component
@RequiredArgsConstructor
public class BudgetCycleBackfillListener {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final BudgetCycleService budgetCycleService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInitialBackfillCompleted(InitialCardBackfillCompletedEvent event) {
        Member member = memberRepository.findById(event.memberId()).orElse(null);
        if (member == null || !member.hasSpendingGuideSetup()) {
            return;
        }
        budgetCycleService.backfillPastCycles(member, LocalDate.now(ZONE_SEOUL));
    }
}