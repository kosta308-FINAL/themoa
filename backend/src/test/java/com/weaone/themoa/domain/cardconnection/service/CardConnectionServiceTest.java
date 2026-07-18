package com.weaone.themoa.domain.cardconnection.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.cardconnection.client.CodefAccountResult;
import com.weaone.themoa.domain.cardconnection.client.CodefCardConnectionClient;
import com.weaone.themoa.domain.cardconnection.client.CodefClientException;
import com.weaone.themoa.domain.cardconnection.dto.request.CardConnectionCreateRequest;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionListResponse;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionResponse;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.CodefValueType;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionAttempt;
import com.weaone.themoa.domain.cardconnection.event.CardConnectionEstablishedEvent;
import com.weaone.themoa.domain.cardconnection.event.CardSyncResumedEvent;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardconnection.repository.ConnectionAttemptRepository;
import com.weaone.themoa.domain.member.entity.EntryMode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CardConnectionServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ORGANIZATION_SHINHAN = "0306";
    private static final String ORGANIZATION_HYUNDAI = "0302";
    private static final String ORGANIZATION_WOORI = "0309";

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CardIssuerRepository cardIssuerRepository;
    @Mock
    private CardConnectionRepository cardConnectionRepository;
    @Mock
    private ConnectionAttemptRepository connectionAttemptRepository;
    @Mock
    private ConnectionAttemptService connectionAttemptService;
    @Mock
    private CardConnectionLockService cardConnectionLockService;
    @Mock
    private CodefCardConnectionClient codefCardConnectionClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CardConnectionService cardConnectionService;

    private CardIssuer shinhan() {
        return CardIssuer.seed(ORGANIZATION_SHINHAN, "신한카드", CodefValueType.TYPE1, CodefValueType.TYPE1, true, false);
    }

    private CardIssuer hyundai() {
        return CardIssuer.seed(ORGANIZATION_HYUNDAI, "현대카드", CodefValueType.TYPE2, CodefValueType.TYPE1, false, false);
    }

    private CardIssuer woori() {
        return CardIssuer.seed(ORGANIZATION_WOORI, "우리카드", CodefValueType.TYPE2, CodefValueType.TYPE1, false, false);
    }

    private CardConnectionCreateRequest request(String organization) {
        return new CardConnectionCreateRequest(organization, "loginId", "loginPassword", null, null, null);
    }

    @Test
    @DisplayName("지원하지 않는 카드사는 400으로 거부하고 CODEF를 호출하지 않는다")
    void rejectsUnsupportedOrganization() {
        given(cardIssuerRepository.findById("9999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request("9999")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_ISSUER_NOT_SUPPORTED);
        then(codefCardConnectionClient).should(never()).createAccount(any());
    }

    @Test
    @DisplayName("현대카드는 카드번호·카드비밀번호가 없으면 400으로 거부한다")
    void rejectsHyundaiWithoutCardInfo() {
        given(cardIssuerRepository.findById(ORGANIZATION_HYUNDAI)).willReturn(Optional.of(hyundai()));

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_HYUNDAI)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        then(codefCardConnectionClient).should(never()).createAccount(any());
    }

    @Test
    @DisplayName("이미 활성 연결이 있으면 409로 거부한다")
    void rejectsAlreadyActiveConnection() {
        CardIssuer cardIssuer = shinhan();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.of(CardConnection.connect(null, cardIssuer, "connected-id", LocalDateTime.now())));

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_CONNECTION_ALREADY_EXISTS);
        then(codefCardConnectionClient).should(never()).createAccount(any());
    }

    @Test
    @DisplayName("쿨다운 중이면 429로 거부한다")
    void rejectsWhenCoolingDown() {
        CardIssuer cardIssuer = shinhan();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        ConnectionAttempt attempt = ConnectionAttempt.start(null, cardIssuer);
        attempt.recordFailure(LocalDateTime.now(), 1, Duration.ofMinutes(5));
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.of(attempt));

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_CONNECTION_COOLDOWN);
        then(codefCardConnectionClient).should(never()).createAccount(any());
    }

    @Test
    @DisplayName("신규 연결 성공 시 ACTIVE 상태로 저장하고 실패 카운트를 리셋한다")
    void connectsSuccessfully() {
        CardIssuer cardIssuer = shinhan();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willReturn(CodefAccountResult.success("connected-id-1", "CF-00000", "정상"));
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);

        CardConnectionResponse response = cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN));

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.organization()).isEqualTo(ORGANIZATION_SHINHAN);
        then(cardConnectionRepository).should().save(any(CardConnection.class));
        then(connectionAttemptService).should().reset(MEMBER_ID, ORGANIZATION_SHINHAN);
        // 수기→카드 전환(entryMode.md §2)과 최초 백필 트리거(§3)가 함께 일어난다.
        assertThat(member.getEntryMode()).isEqualTo(EntryMode.CARD);
        then(eventPublisher).should().publishEvent(any(CardConnectionEstablishedEvent.class));
    }

    @Test
    @DisplayName("재연결은 entry_mode 전환·백필 이벤트를 발행하지 않는다")
    void reconnectDoesNotPublishEstablishedEvent() {
        CardIssuer cardIssuer = shinhan();
        CardConnection existing = CardConnection.connect(null, cardIssuer, "old-id", LocalDateTime.now());
        existing.markLocked();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.of(existing));
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willReturn(CodefAccountResult.success("new-id", "CF-00000", "정상"));

        cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN));

        then(eventPublisher).should(never()).publishEvent(any(CardConnectionEstablishedEvent.class));
    }

    @Test
    @DisplayName("자동수집을 OFF에서 ON으로 바꾸면 갭 백필 이벤트를 발행한다")
    void resumingCardSyncPublishesGapBackfillEvent() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        member.disableCardSync();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        cardConnectionService.setCardSyncEnabled(MEMBER_ID, true);

        assertThat(member.isCardSyncEnabled()).isTrue();
        then(eventPublisher).should().publishEvent(any(CardSyncResumedEvent.class));
    }

    @Test
    @DisplayName("이미 ON인 상태에서 다시 ON을 요청해도 갭 백필 이벤트를 중복 발행하지 않는다")
    void enablingAlreadyEnabledSyncDoesNotPublishEvent() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        cardConnectionService.setCardSyncEnabled(MEMBER_ID, true);

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("자동수집 OFF는 백필 이벤트를 발행하지 않는다")
    void disablingCardSyncDoesNotPublishEvent() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        cardConnectionService.setCardSyncEnabled(MEMBER_ID, false);

        assertThat(member.isCardSyncEnabled()).isFalse();
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("카드 관리 목록은 자동수집 상태와 연결된 카드사 목록을 함께 반환한다")
    void listsConnectionsWithCardSyncFlag() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        CardConnection connection = CardConnection.connect(member, shinhan(), "connected-id", LocalDateTime.now());
        given(cardConnectionRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of(connection));

        CardConnectionListResponse response = cardConnectionService.list(MEMBER_ID);

        assertThat(response.cardSyncEnabled()).isTrue();
        assertThat(response.connections()).hasSize(1);
        assertThat(response.connections().get(0).organization()).isEqualTo(ORGANIZATION_SHINHAN);
    }

    @Test
    @DisplayName("LOCKED 상태였던 연결이 재연결에 성공하면 같은 행을 갱신하고 새로 저장하지 않는다")
    void reconnectsExistingConnection() {
        CardIssuer cardIssuer = shinhan();
        CardConnection existing = CardConnection.connect(null, cardIssuer, "old-id", LocalDateTime.now());
        existing.markLocked();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.of(existing));
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willReturn(CodefAccountResult.success("new-id", "CF-00000", "정상"));

        CardConnectionResponse response = cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN));

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(existing.getConnectedId()).isEqualTo("new-id");
        then(cardConnectionRepository).should(never()).save(any(CardConnection.class));
    }

    @Test
    @DisplayName("비밀번호 오류(CF-12801)는 401로 응답하고 실패를 기록한다")
    void recordsFailureOnInvalidPassword() {
        CardIssuer cardIssuer = shinhan();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willReturn(CodefAccountResult.failure("CF-12801", "비밀번호 오류", ""));
        given(connectionAttemptService.recordFailure(eq(MEMBER_ID), eq(cardIssuer), any(LocalDateTime.class)))
                .willReturn(false);

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_CONNECTION_LOGIN_FAILED);
        then(connectionAttemptService).should().recordFailure(eq(MEMBER_ID), eq(cardIssuer), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("3번째 비밀번호 오류로 쿨다운이 걸리면 429로 응답한다")
    void triggersCooldownOnThirdFailure() {
        CardIssuer cardIssuer = shinhan();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willReturn(CodefAccountResult.failure("CF-12801", "비밀번호 오류", ""));
        given(connectionAttemptService.recordFailure(eq(MEMBER_ID), eq(cardIssuer), any(LocalDateTime.class)))
                .willReturn(true);

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_CONNECTION_COOLDOWN);
    }

    @Test
    @DisplayName("카드사 계정 잠금 신호(userError=99)를 받으면 기존 연결을 LOCKED로 표시하고 423으로 응답한다")
    void marksLockedOnUserErrorLimitImminent() {
        CardIssuer cardIssuer = shinhan();
        CardConnection existing = CardConnection.connect(null, cardIssuer, "old-id", LocalDateTime.now());
        // 재연결 시도 상황을 재현한다. 기존 연결이 ACTIVE면 CODEF 호출 전에 이미 CARD_CONNECTION_ALREADY_EXISTS로 막힌다.
        existing.markLocked();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.of(existing));
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willReturn(CodefAccountResult.failure("CF-12801", "비밀번호 오류", "99"));

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_CONNECTION_LOCKED);
        then(cardConnectionLockService).should().markLocked(existing.getId());
        then(connectionAttemptService).should(never()).recordFailure(any(), any(), any());
    }

    @Test
    @DisplayName("우리카드는 제한직전(userError=99)이어도 잠금 대신 생년월일 추가입력을 요구한다")
    void requestsBirthDateForWooriInsteadOfLocking() {
        CardIssuer cardIssuer = woori();
        given(cardIssuerRepository.findById(ORGANIZATION_WOORI)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_WOORI))
                .willReturn(Optional.empty());
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_WOORI))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willReturn(CodefAccountResult.failure("CF-12801", "비밀번호 오류", "99"));

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_WOORI)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_CONNECTION_BIRTHDATE_REQUIRED);
        then(cardConnectionLockService).should(never()).markLocked(any());
    }

    @Test
    @DisplayName("CODEF 통신 실패는 502로 변환한다")
    void wrapsCodefClientException() {
        CardIssuer cardIssuer = shinhan();
        given(cardIssuerRepository.findById(ORGANIZATION_SHINHAN)).willReturn(Optional.of(cardIssuer));
        given(cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(MEMBER_ID, ORGANIZATION_SHINHAN))
                .willReturn(Optional.empty());
        given(codefCardConnectionClient.createAccount(any()))
                .willThrow(new CodefClientException("통신 실패", new RuntimeException("io")));

        assertThatThrownBy(() -> cardConnectionService.connect(MEMBER_ID, request(ORGANIZATION_SHINHAN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARD_CONNECTION_EXTERNAL_ERROR);
    }
}
