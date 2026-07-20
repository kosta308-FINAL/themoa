package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardconnection.client.CodefClientException;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.entity.InitialSyncStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalListClient;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalListCommand;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalRecord;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionSource;
import com.weaone.themoa.domain.cardtransaction.event.InitialCardBackfillCompletedEvent;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.cardtransaction.support.BackfillWindowPolicy;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.notification.entity.NotificationType;
import com.weaone.themoa.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 카드 백필(entryMode.md §3·§4·§4-1) 실행 + 갭 구간 수기 카드 건 대체. 신규 커넥션 최초 백필과 자동수집
 * 재개(OFF→ON) 갭 백필은 "카드 수집 재개 시 갭 구간 대체"라는 같은 일반 규칙을 공유한다(§4-1) — 이 서비스가
 * 그 단일 구현이다. 트리거는 cardconnection 도메인이 발행하는 이벤트({@link
 * com.weaone.themoa.domain.cardconnection.event.CardConnectionEstablishedEvent},
 * {@link com.weaone.themoa.domain.cardconnection.event.CardSyncResumedEvent})를 통해서만 들어온다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionBackfillService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final String ERROR_CODE_EXTERNAL = "BACKFILL_EXTERNAL_ERROR";
    private static final String ERROR_CODE_STALE = "BACKFILL_STALE_RESTART";
    private static final long STALE_INITIAL_SYNC_MINUTES = 30;

    private final CardConnectionRepository cardConnectionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CodefApprovalListClient codefApprovalListClient;
    private final CardTransactionCollectionService cardTransactionCollectionService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    /** 신규 커넥션 최초 백필(§3): 시작일 = (오늘이 속한 달의 1일) − BACKFILL_MONTHS. 커넥션마다 독립적으로 1회만 실행된다. */
    @Transactional
    public void runInitialBackfill(Long connectionId) {
        triggerInitialBackfill(connectionId, InitialSyncStatus.NOT_STARTED);
    }

    /** 초기수집 재시도(dayguide.md §8.1·§8.5): FAILED 상태에서만 FETCHING부터 다시 시작한다. */
    @Transactional
    public void retryInitialBackfill(Long connectionId) {
        triggerInitialBackfill(connectionId, InitialSyncStatus.FAILED);
    }

    /** 이벤트 발행 시점과 처리 시점 사이 상태가 바뀔 수 있어 실행 직전 상태를 다시 검증한다(멱등 가드). */
    private void triggerInitialBackfill(Long connectionId, InitialSyncStatus requiredStatus) {
        CardConnection connection = cardConnectionRepository.findById(connectionId).orElse(null);
        if (connection == null || connection.getInitialSyncStatus() != requiredStatus) {
            return;
        }
        LocalDate end = LocalDate.now(ZONE_SEOUL);
        LocalDate start = BackfillWindowPolicy.calendarFloor(end);
        backfill(connection, start, end);
        if (connection.getInitialSyncStatus() == InitialSyncStatus.COMPLETED) {
            // 습관성 지출 코칭 최초 즉시 생성 트리거(habitExpense.md §3). 갭 백필에는 발행하지 않는다.
            eventPublisher.publishEvent(new InitialCardBackfillCompletedEvent(connection.getMember().getId()));
        }
    }

    /**
     * 자동수집 재개(§2-1, §4-1) 시 회원의 활성 커넥션 전체를 갭 구간만큼 백필한다. {@code recoveryMode}가
     * {@code null}이면 기존 자동 판단(RECOVER_RECENT와 동일)을 그대로 쓴다(dayguide.md §8.1).
     */
    @Transactional
    public void runGapBackfillForMember(Long memberId, RecoveryMode recoveryMode) {
        LocalDate end = LocalDate.now(ZONE_SEOUL);
        List<CardConnection> connections = cardConnectionRepository
                .findByMember_IdAndStatus(memberId, ConnectionStatus.ACTIVE);
        for (CardConnection connection : connections) {
            backfill(connection, resolveGapStart(connection, end, recoveryMode), end);
        }
    }

    /**
     * 갭 시작(§4-1): {@code recoveryMode=CURRENT_MONTH}면 복귀일이 속한 달의 1일부터(과거 공백은 채우지
     * 않음, cardtransaction.md §6-C와 동일 정의). 그 외(RECOVER_RECENT 또는 미지정)는
     * {@code last_successful_sync_at} 기준으로, 갭이 3개월을 넘으면 §6-(C) 복귀 상한과 같은 계산식으로
     * 캡을 씌운다 — 별도 상수를 두지 않는다(entryMode.md §3 각주).
     */
    private LocalDate resolveGapStart(CardConnection connection, LocalDate end, RecoveryMode recoveryMode) {
        if (recoveryMode == RecoveryMode.CURRENT_MONTH) {
            return end.withDayOfMonth(1);
        }
        LocalDate calendarCap = BackfillWindowPolicy.calendarFloor(end);
        LocalDateTime lastSuccessfulSyncAt = connection.getLastSuccessfulSyncAt();
        if (lastSuccessfulSyncAt == null) {
            return calendarCap;
        }
        LocalDate lastSyncDate = lastSuccessfulSyncAt.toLocalDate();
        return lastSyncDate.isAfter(calendarCap) ? lastSyncDate : calendarCap;
    }

    private void backfill(CardConnection connection, LocalDate start, LocalDate end) {
        Member member = connection.getMember();
        CardIssuer cardIssuer = connection.getCardIssuer();
        LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
        connection.startInitialSync(now);

        List<CodefApprovalRecord> records;
        try {
            records = codefApprovalListClient.fetch(new CodefApprovalListCommand(
                    cardIssuer.getOrganization(), connection.getConnectedId(), null, start, end));
        } catch (CodefClientException e) {
            log.warn("백필 조회 실패. connectionId={}", connection.getId(), e);
            connection.failInitialSync(ERROR_CODE_EXTERNAL);
            return;
        }

        for (CodefApprovalRecord record : records) {
            try {
                cardTransactionCollectionService.collect(member, connection, cardIssuer, record);
            } catch (RuntimeException e) {
                log.warn("백필 1건 처리 실패, 다음 건으로 계속 진행합니다. connectionId={}, approvalNo={}",
                        connection.getId(), record.resApprovalNo(), e);
            }
        }

        connection.markInitialSyncAnalyzing();
        ReplacementSummary summary = replaceManualCardEntries(member, start, end, now);
        connection.completeInitialSync(now);
        connection.markSynced(now);

        if (summary.replacedCount() > 0) {
            notifyBackfillRecalculated(member, connection, start, end);
        }
        if (summary.hasUnpaired()) {
            notifyUnlinkedCardSuspected(member);
        }
    }

    /**
     * 갭 구간의 결제수단=카드 수기 건을 대체한다(§4/§4-1). 대체 여부의 판별자는 결제수단 태그뿐이고
     * (fuzzy 매칭 아님, §4), 짝이 되는 카드 거래는 같은 날짜·금액 정확 일치로만 찾는다(§4-2) — 못 찾으면
     * NULL로 남겨 soft 보존한다(물리 삭제 금지).
     */
    private ReplacementSummary replaceManualCardEntries(Member member, LocalDate start, LocalDate end,
                                                          LocalDateTime now) {
        List<CardTransaction> manualCardEntries = cardTransactionRepository
                .findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(
                        member.getId(), TransactionSource.MANUAL, PaymentMethod.CARD, start, end);

        int replaced = 0;
        boolean hasUnpaired = false;
        for (CardTransaction manualEntry : manualCardEntries) {
            CardTransaction match = cardTransactionRepository
                    .findFirstByMember_IdAndSourceAndUsedDateAndAmountOrderByIdAsc(
                            member.getId(), TransactionSource.SYNC, manualEntry.getUsedDate(), manualEntry.getAmount())
                    .orElse(null);
            manualEntry.replace(match, now);
            replaced++;
            if (match == null) {
                hasUnpaired = true;
            }
        }
        return new ReplacementSummary(replaced, hasUnpaired);
    }

    /** 백필로 과거 달 통계가 바뀌었음을 알린다(§3-1) — 숫자가 말없이 바뀌는 것만 막는다. */
    private void notifyBackfillRecalculated(Member member, CardConnection connection, LocalDate start, LocalDate end) {
        String message = start + " ~ " + end + " 사이의 카드 내역이 반영되어 이전 소비 통계가 갱신되었습니다.";
        String dedupKey = "BACKFILL_RECALCULATED:conn=" + connection.getId() + ":start=" + start;
        notificationService.createIfAbsent(member, NotificationType.BACKFILL_RECALCULATED, message, null, dedupKey);
    }

    /** 짝 없는 대체 = 연동하지 않은 카드가 있다는 신호(§4-2). 별도 탐지 로직 없이 이 조건으로 안내한다. */
    private void notifyUnlinkedCardSuspected(Member member) {
        String message = "연동되지 않은 카드로 결제한 내역이 있을 수 있습니다. 사용 중인 카드를 모두 연결해 주세요.";
        String dedupKey = "UNLINKED_CARD_SUSPECTED:member=" + member.getId();
        notificationService.createIfAbsent(member, NotificationType.UNLINKED_CARD_SUSPECTED, message, null, dedupKey);
    }

    /** 서버 재시작 등으로 FETCHING/ANALYZING에 오래 멈춘 백필을 FAILED로 정리한다(erd.md 카드사 커넥션 §비고). */
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    @Transactional
    public void failStaleInitialSyncs() {
        LocalDateTime staleBefore = LocalDateTime.now(ZONE_SEOUL).minusMinutes(STALE_INITIAL_SYNC_MINUTES);
        List<CardConnection> stale = cardConnectionRepository.findByInitialSyncStatusInAndInitialSyncStartedAtBefore(
                List.of(InitialSyncStatus.FETCHING, InitialSyncStatus.ANALYZING), staleBefore);
        stale.forEach(connection -> connection.failInitialSync(ERROR_CODE_STALE));
    }

    private record ReplacementSummary(int replacedCount, boolean hasUnpaired) {
    }
}
