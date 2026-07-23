package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.cardconnection.client.CodefClientException;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalListClient;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalListCommand;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalRecord;
import com.weaone.themoa.domain.cardtransaction.support.BackfillWindowPolicy;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 수집 트리거(cardtransaction.md §6): 온디맨드(짧은 윈도우) + 새벽 배치(넓은 윈도우, 최근 이용자만) +
 * 30일 초과 복귀 동기화. "저녁 배치"는 없다 — 두 갈래뿐이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionSyncService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final int ON_DEMAND_WINDOW_DAYS = 2;
    private static final int BATCH_WINDOW_DAYS = 15;
    private static final long ON_DEMAND_THROTTLE_MINUTES = 30;
    private static final long INACTIVITY_LIMIT_DAYS = 30;

    private final CardConnectionRepository cardConnectionRepository;
    private final MemberRepository memberRepository;
    private final CodefApprovalListClient codefApprovalListClient;
    private final CardTransactionCollectionService cardTransactionCollectionService;
    private final CardSyncLockService cardSyncLockService;

    /** 앱 열기(자동, 30분 쓰로틀) 또는 수동 새로고침(락만). §6 (A) — 새벽 배치와 동일하게 자동수집 OFF는 제외한다. */
    public SyncSummary syncOnDemand(Long memberId, boolean manual) {
        List<CardConnection> connections = cardConnectionRepository
                .findByMember_IdAndStatusAndMember_CardSyncEnabled(memberId, ConnectionStatus.ACTIVE, true);
        LocalDate end = LocalDate.now(ZONE_SEOUL);
        LocalDate start = end.minusDays(ON_DEMAND_WINDOW_DAYS);
        SyncSummary result = syncForMember(memberId, connections, start, end, !manual);
        // 앱 열기·수동 새로고침도 "이용"이다 — last_active_at을 갱신해야 30일 초과 새벽배치 제외에서 빠지지 않는다(§6-B).
        markMemberActive(memberId);
        return result;
    }

    /** 새벽 저활동 시간대 1회. 마지막 이용 후 30일 이내 사용자만 대상이다. §6 (B). */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runNightlyBatch() {
        // 임시 로깅: 배치 미실행/미동기화 원인 확인용. 원인 파악되면 제거.
        log.info("[카드동기화-배치] 시작. now={}", LocalDateTime.now(ZONE_SEOUL));
        LocalDateTime activeSince = LocalDateTime.now(ZONE_SEOUL).minusDays(INACTIVITY_LIMIT_DAYS);
        List<CardConnection> connections = cardConnectionRepository
                .findEligibleForNightlyBatch(ConnectionStatus.ACTIVE, activeSince);
        LocalDate end = LocalDate.now(ZONE_SEOUL);
        LocalDate start = end.minusDays(BATCH_WINDOW_DAYS);

        Map<Long, List<CardConnection>> byMember = connections.stream()
                .collect(Collectors.groupingBy(connection -> connection.getMember().getId()));
        log.info("[카드동기화-배치] 대상 커넥션={}건, 대상 회원={}명, window={}~{}",
                connections.size(), byMember.size(), start, end);

        byMember.forEach((memberId, memberConnections) -> {
            try {
                SyncSummary result = syncForMember(memberId, memberConnections, start, end, false);
                log.info("[카드동기화-배치] memberId={} 완료. created={}, updated={}, skipped={}, locked={}",
                        memberId, result.created(), result.updated(), result.skipped(), result.locked());
            } catch (RuntimeException e) {
                log.error("[카드동기화-배치] memberId={} 처리 중 예외 발생, 다음 회원으로 계속 진행합니다.", memberId, e);
            }
        });
        log.info("[카드동기화-배치] 종료. 처리 회원 수={}", byMember.size());
    }

    @Transactional(readOnly = true)
    public boolean isReturningAfterLongAbsence(Long memberId) {
        Member member = getMember(memberId);
        return member.isReturningAfterAbsence(LocalDateTime.now(ZONE_SEOUL), INACTIVITY_LIMIT_DAYS);
    }

    /** 30일 초과 복귀자 전용 분기. §6-C. recovery-status는 클라이언트 UI 판단용일 뿐이라 여기서 다시 검증한다. */
    public SyncSummary recover(Long memberId, RecoveryMode mode) {
        if (!isReturningAfterLongAbsence(memberId)) {
            throw new BusinessException(ErrorCode.CARD_SYNC_RECOVERY_NOT_ELIGIBLE);
        }
        List<CardConnection> connections = cardConnectionRepository
                .findByMember_IdAndStatusAndMember_CardSyncEnabled(memberId, ConnectionStatus.ACTIVE, true);
        LocalDate end = LocalDate.now(ZONE_SEOUL);

        if (!cardSyncLockService.tryAcquire(memberId)) {
            return SyncSummary.lockedResult();
        }
        SyncSummary total = SyncSummary.empty();
        try {
            for (CardConnection connection : connections) {
                LocalDate start = resolveRecoveryStart(connection, mode, end);
                total = total.plus(syncOneConnection(connection, start, end));
            }
        } finally {
            cardSyncLockService.release(memberId);
        }
        markMemberActive(memberId);
        return total;
    }

    private SyncSummary syncForMember(Long memberId, List<CardConnection> connections,
                                       LocalDate start, LocalDate end, boolean applyThrottle) {
        if (!cardSyncLockService.tryAcquire(memberId)) {
            return SyncSummary.lockedResult();
        }
        try {
            SyncSummary total = SyncSummary.empty();
            for (CardConnection connection : connections) {
                if (applyThrottle && isThrottled(connection)) {
                    continue;
                }
                total = total.plus(syncOneConnection(connection, start, end));
            }
            return total;
        } finally {
            cardSyncLockService.release(memberId);
        }
    }

    private boolean isThrottled(CardConnection connection) {
        LocalDateTime lastSync = connection.getLastSuccessfulSyncAt();
        return lastSync != null
                && lastSync.isAfter(LocalDateTime.now(ZONE_SEOUL).minusMinutes(ON_DEMAND_THROTTLE_MINUTES));
    }

    private SyncSummary syncOneConnection(CardConnection connection, LocalDate start, LocalDate end) {
        Member member = connection.getMember();
        CardIssuer cardIssuer = connection.getCardIssuer();

        List<CodefApprovalRecord> records;
        try {
            records = codefApprovalListClient.fetch(new CodefApprovalListCommand(
                    cardIssuer.getOrganization(), connection.getConnectedId(), null, start, end));
        } catch (CodefClientException e) {
            log.warn("승인내역 조회 실패, 이 커넥션은 이번 수집에서 건너뜁니다. connectionId={}", connection.getId());
            return SyncSummary.empty();
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (CodefApprovalRecord record : records) {
            try {
                switch (cardTransactionCollectionService.collect(member, connection, cardIssuer, record)) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                }
            } catch (RuntimeException e) {
                log.warn("승인내역 1건 처리 실패, 다음 건으로 계속 진행합니다. connectionId={}, approvalNo={}",
                        connection.getId(), record.resApprovalNo(), e);
                skipped++;
            }
        }
        if (skipped == 0) {
            // 부분 실패(건별 예외·환율 미확보)가 있으면 last_successful_sync_at을 보존한다 — "전체 성공 후에만" 갱신(§6-C).
            markConnectionSynced(connection.getId());
        }
        return new SyncSummary(created, updated, skipped, false);
    }

    /** "3개월"은 달력 월 상한이다 — 복귀 월 1일 기준으로 3개월 전 1일까지만 소급한다(§6-C). */
    private LocalDate resolveRecoveryStart(CardConnection connection, RecoveryMode mode, LocalDate end) {
        LocalDate firstDayOfMonth = end.withDayOfMonth(1);
        if (mode == RecoveryMode.CURRENT_MONTH) {
            return firstDayOfMonth;
        }
        LocalDate calendarCap = BackfillWindowPolicy.calendarFloor(end);
        LocalDateTime lastSuccessfulSyncAt = connection.getLastSuccessfulSyncAt();
        if (lastSuccessfulSyncAt == null) {
            return calendarCap;
        }
        LocalDate lastSyncDate = lastSuccessfulSyncAt.toLocalDate();
        return lastSyncDate.isAfter(calendarCap) ? lastSyncDate : calendarCap;
    }

    @Transactional
    public void markConnectionSynced(Long connectionId) {
        cardConnectionRepository.findById(connectionId)
                .ifPresent(connection -> connection.markSynced(LocalDateTime.now(ZONE_SEOUL)));
    }

    @Transactional
    public void markMemberActive(Long memberId) {
        getMember(memberId).markActive(LocalDateTime.now(ZONE_SEOUL));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }
}
