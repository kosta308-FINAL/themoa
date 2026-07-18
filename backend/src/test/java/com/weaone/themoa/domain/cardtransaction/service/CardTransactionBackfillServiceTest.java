package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardconnection.client.CodefClientException;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.CodefValueType;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.entity.InitialSyncStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalListClient;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalListCommand;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionSource;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.notification.entity.NotificationType;
import com.weaone.themoa.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CardTransactionBackfillServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CONNECTION_ID = 10L;

    @Mock
    private CardConnectionRepository cardConnectionRepository;
    @Mock
    private CardTransactionRepository cardTransactionRepository;
    @Mock
    private CodefApprovalListClient codefApprovalListClient;
    @Mock
    private CardTransactionCollectionService cardTransactionCollectionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CardTransactionBackfillService cardTransactionBackfillService;

    private Member member() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private Category category() {
        return Category.seed(CategoryCode.FOOD, "식비");
    }

    private CardConnection connection(Member member) {
        CardIssuer cardIssuer = CardIssuer.seed("0306", "신한카드", CodefValueType.TYPE1, CodefValueType.TYPE1, true, false);
        CardConnection connection = CardConnection.connect(member, cardIssuer, "connected-id", LocalDateTime.now());
        ReflectionTestUtils.setField(connection, "id", CONNECTION_ID);
        return connection;
    }

    private void stubNoManualEntries() {
        given(cardTransactionRepository.findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(
                eq(MEMBER_ID), eq(TransactionSource.MANUAL), eq(PaymentMethod.CARD), any(), any()))
                .willReturn(List.of());
    }

    @Test
    @DisplayName("이미 NOT_STARTED가 아니면 최초 백필을 다시 실행하지 않는다")
    void skipsInitialBackfillWhenAlreadyStarted() {
        CardConnection connection = connection(member());
        connection.startInitialSync(LocalDateTime.now());
        given(cardConnectionRepository.findById(CONNECTION_ID)).willReturn(Optional.of(connection));

        cardTransactionBackfillService.runInitialBackfill(CONNECTION_ID);

        then(codefApprovalListClient).should(never()).fetch(any());
    }

    @Test
    @DisplayName("최초 백필은 조회한 레코드를 전부 수집하고 완료 상태로 남긴다")
    void runsInitialBackfillSuccessfully() {
        CardConnection connection = connection(member());
        given(cardConnectionRepository.findById(CONNECTION_ID)).willReturn(Optional.of(connection));
        given(codefApprovalListClient.fetch(any())).willReturn(List.of());
        stubNoManualEntries();

        cardTransactionBackfillService.runInitialBackfill(CONNECTION_ID);

        assertThat(connection.getInitialSyncStatus()).isEqualTo(InitialSyncStatus.COMPLETED);
        assertThat(connection.getLastSuccessfulSyncAt()).isNotNull();
        then(notificationService).should(never()).createIfAbsent(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("백필 시작일은 (오늘이 속한 달의 1일) − 3개월이다")
    void computesCalendarFloorForInitialBackfill() {
        CardConnection connection = connection(member());
        given(cardConnectionRepository.findById(CONNECTION_ID)).willReturn(Optional.of(connection));
        given(codefApprovalListClient.fetch(any())).willReturn(List.of());
        stubNoManualEntries();

        cardTransactionBackfillService.runInitialBackfill(CONNECTION_ID);

        ArgumentCaptor<CodefApprovalListCommand> captor = ArgumentCaptor.forClass(CodefApprovalListCommand.class);
        then(codefApprovalListClient).should().fetch(captor.capture());
        LocalDate today = LocalDate.now();
        assertThat(captor.getValue().startDate()).isEqualTo(today.withDayOfMonth(1).minusMonths(3));
        assertThat(captor.getValue().endDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("CODEF 조회 실패 시 FAILED로 남기고 이후 단계를 진행하지 않는다")
    void marksFailedOnExternalError() {
        CardConnection connection = connection(member());
        given(cardConnectionRepository.findById(CONNECTION_ID)).willReturn(Optional.of(connection));
        given(codefApprovalListClient.fetch(any())).willThrow(new CodefClientException("실패", null));

        cardTransactionBackfillService.runInitialBackfill(CONNECTION_ID);

        assertThat(connection.getInitialSyncStatus()).isEqualTo(InitialSyncStatus.FAILED);
        assertThat(connection.getInitialSyncErrorCode()).isEqualTo("BACKFILL_EXTERNAL_ERROR");
        then(cardTransactionCollectionService).should(never()).collect(any(), any(), any(), any());
        then(cardTransactionRepository).should(never())
                .findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("갭 구간의 카드 태그 수기 건은 같은 날짜·금액의 카드 거래로 대체되고 재계산 알림이 발생한다")
    void replacesManualCardEntryWithMatchingSyncTransaction() {
        Member member = member();
        CardConnection connection = connection(member);
        given(cardConnectionRepository.findById(CONNECTION_ID)).willReturn(Optional.of(connection));
        given(codefApprovalListClient.fetch(any())).willReturn(List.of());

        CardTransaction manualEntry = CardTransaction.manual(member, category(), PaymentMethod.CARD,
                LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 10, 12, 0),
                BigDecimal.valueOf(9000), "카드결제", null);
        CardTransaction syncMatch = CardTransaction.sync(member, null, category(), "12345678",
                LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 10, 12, 5), BigDecimal.valueOf(9000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false,
                "스타벅스", null, null, null, null);

        given(cardTransactionRepository.findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(
                eq(MEMBER_ID), eq(TransactionSource.MANUAL), eq(PaymentMethod.CARD), any(), any()))
                .willReturn(List.of(manualEntry));
        given(cardTransactionRepository.findFirstByMember_IdAndSourceAndUsedDateAndAmountOrderByIdAsc(
                MEMBER_ID, TransactionSource.SYNC, manualEntry.getUsedDate(), manualEntry.getAmount()))
                .willReturn(Optional.of(syncMatch));

        cardTransactionBackfillService.runInitialBackfill(CONNECTION_ID);

        assertThat(manualEntry.getReplacedAt()).isNotNull();
        assertThat(manualEntry.getReplacedByTransaction()).isEqualTo(syncMatch);
        then(notificationService).should().createIfAbsent(eq(member), eq(NotificationType.BACKFILL_RECALCULATED),
                any(), eq(null), any());
        then(notificationService).should(never())
                .createIfAbsent(any(), eq(NotificationType.UNLINKED_CARD_SUSPECTED), any(), any(), any());
    }

    @Test
    @DisplayName("짝을 못 찾은 대체는 물리 삭제되지 않고 미연동 카드 의심 알림을 보낸다")
    void notifiesUnlinkedCardWhenNoMatchFound() {
        Member member = member();
        CardConnection connection = connection(member);
        given(cardConnectionRepository.findById(CONNECTION_ID)).willReturn(Optional.of(connection));
        given(codefApprovalListClient.fetch(any())).willReturn(List.of());

        CardTransaction manualEntry = CardTransaction.manual(member, category(), PaymentMethod.CARD,
                LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 10, 12, 0),
                BigDecimal.valueOf(9000), "카드결제", null);

        given(cardTransactionRepository.findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(
                eq(MEMBER_ID), eq(TransactionSource.MANUAL), eq(PaymentMethod.CARD), any(), any()))
                .willReturn(List.of(manualEntry));
        given(cardTransactionRepository.findFirstByMember_IdAndSourceAndUsedDateAndAmountOrderByIdAsc(
                MEMBER_ID, TransactionSource.SYNC, manualEntry.getUsedDate(), manualEntry.getAmount()))
                .willReturn(Optional.empty());

        cardTransactionBackfillService.runInitialBackfill(CONNECTION_ID);

        assertThat(manualEntry.getReplacedAt()).isNotNull();
        assertThat(manualEntry.getReplacedByTransaction()).isNull();
        then(notificationService).should().createIfAbsent(eq(member), eq(NotificationType.UNLINKED_CARD_SUSPECTED),
                any(), eq(null), any());
    }

    @Test
    @DisplayName("갭 백필은 활성 커넥션의 last_successful_sync_at부터 오늘까지 조회한다")
    void runsGapBackfillFromLastSuccessfulSync() {
        CardConnection connection = connection(member());
        connection.markSynced(LocalDateTime.of(2026, 6, 1, 0, 0));
        given(cardConnectionRepository.findByMember_IdAndStatus(MEMBER_ID, ConnectionStatus.ACTIVE))
                .willReturn(List.of(connection));
        given(codefApprovalListClient.fetch(any())).willReturn(List.of());
        stubNoManualEntries();

        cardTransactionBackfillService.runGapBackfillForMember(MEMBER_ID);

        ArgumentCaptor<CodefApprovalListCommand> captor = ArgumentCaptor.forClass(CodefApprovalListCommand.class);
        then(codefApprovalListClient).should().fetch(captor.capture());
        assertThat(captor.getValue().startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    }
}
