package com.weaone.themoa.domain.cardconnection.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.cardconnection.client.CodefAccountResult;
import com.weaone.themoa.domain.cardconnection.client.CodefCardConnectionClient;
import com.weaone.themoa.domain.cardconnection.client.CodefClientException;
import com.weaone.themoa.domain.cardconnection.client.CodefCreateAccountCommand;
import com.weaone.themoa.domain.cardconnection.dto.request.CardConnectionCreateRequest;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionListResponse;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionResponse;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.event.CardConnectionEstablishedEvent;
import com.weaone.themoa.domain.cardconnection.event.CardSyncResumedEvent;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardconnection.repository.ConnectionAttemptRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 카드사 커넥션(connectedId) 등록. 결제내역 수집 자체는 cardtransaction.md 소관이라 여기서 다루지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardConnectionService {

    /** connection.md §3-1 — ID 로그인 파라미터상 카드번호+카드비밀번호가 필수인 카드사는 현대뿐이다. */
    private static final String ORGANIZATION_HYUNDAI = "0302";

    /** connection.md §5-3 — 제한직전 응답이 하드락이 아니라 birthDate 추가입력으로 재개되는 유일한 카드사. */
    private static final String ORGANIZATION_WOORI = "0309";

    private static final String RESULT_CODE_PASSWORD_INVALID = "CF-12801";

    /** connection.md §5-2 — 카드사 계정 잠금 임박/발생 신호. 우리 쿨다운과 별개이며 우리가 풀 수 없다. */
    private static final String USER_ERROR_LIMIT_IMMINENT = "99";

    private final MemberRepository memberRepository;
    private final CardIssuerRepository cardIssuerRepository;
    private final CardConnectionRepository cardConnectionRepository;
    private final ConnectionAttemptRepository connectionAttemptRepository;
    private final ConnectionAttemptService connectionAttemptService;
    private final CardConnectionLockService cardConnectionLockService;
    private final CodefCardConnectionClient codefCardConnectionClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CardConnectionResponse connect(Long memberId, CardConnectionCreateRequest request) {
        CardIssuer cardIssuer = cardIssuerRepository.findById(request.organization())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_ISSUER_NOT_SUPPORTED));
        validateIssuerSpecificFields(cardIssuer, request);

        CardConnection existing = cardConnectionRepository
                .findByMember_IdAndCardIssuer_Organization(memberId, cardIssuer.getOrganization())
                .orElse(null);
        if (existing != null && existing.getStatus() == ConnectionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CARD_CONNECTION_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        rejectIfCoolingDown(memberId, cardIssuer, now);

        CodefAccountResult result = requestConnectedId(cardIssuer, request);
        if (!result.success()) {
            handleFailure(memberId, cardIssuer, existing, result, now);
        }
        connectionAttemptService.reset(memberId, cardIssuer.getOrganization());

        if (existing != null) {
            existing.reconnect(result.connectedId());
            return CardConnectionResponse.from(existing);
        }

        Member member = memberRepository.getReferenceById(memberId);
        // 수기→카드 전환(entryMode.md §2). 이미 CARD면 아무 효과가 없다 — 역전이도 재전환도 없다(§2-1).
        member.startCardSync(now);
        CardConnection connection = CardConnection.connect(member, cardIssuer, result.connectedId(), now);
        cardConnectionRepository.save(connection);
        // 최초 3개월 백필 트리거(§3). 커밋 후 비동기로 실행되어 이 응답을 붙잡지 않는다.
        eventPublisher.publishEvent(new CardConnectionEstablishedEvent(connection.getId()));
        return CardConnectionResponse.from(connection);
    }

    @Transactional(readOnly = true)
    public CardConnectionListResponse list(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        List<CardConnectionResponse> connections = cardConnectionRepository.findByMember_Id(memberId).stream()
                .map(CardConnectionResponse::from)
                .toList();
        return new CardConnectionListResponse(member.isCardSyncEnabled(), connections);
    }

    /** 카드 자동수집 ON/OFF(entryMode.md §2-1). false→true 전환 시에만 갭 백필(§4-1)을 트리거한다. */
    @Transactional
    public void setCardSyncEnabled(Long memberId, boolean enabled) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        boolean resuming = enabled && !member.isCardSyncEnabled();
        if (enabled) {
            member.enableCardSync();
        } else {
            member.disableCardSync();
        }
        if (resuming) {
            eventPublisher.publishEvent(new CardSyncResumedEvent(memberId));
        }
    }

    private void rejectIfCoolingDown(Long memberId, CardIssuer cardIssuer, LocalDateTime now) {
        connectionAttemptRepository.findByMember_IdAndCardIssuer_Organization(memberId, cardIssuer.getOrganization())
                .ifPresent(attempt -> {
                    attempt.releaseCooldownIfExpired(now);
                    if (attempt.isCoolingDown(now)) {
                        throw new BusinessException(ErrorCode.CARD_CONNECTION_COOLDOWN);
                    }
                });
    }

    private CodefAccountResult requestConnectedId(CardIssuer cardIssuer, CardConnectionCreateRequest request) {
        try {
            return codefCardConnectionClient.createAccount(new CodefCreateAccountCommand(
                    cardIssuer.getOrganization(),
                    request.loginId(),
                    request.loginPassword(),
                    request.cardNo(),
                    request.cardPassword(),
                    request.birthDate()
            ));
        } catch (CodefClientException e) {
            log.warn("CODEF 커넥션 등록 요청 실패: organization={}", cardIssuer.getOrganization(), e);
            throw new BusinessException(ErrorCode.CARD_CONNECTION_EXTERNAL_ERROR);
        }
    }

    private void validateIssuerSpecificFields(CardIssuer cardIssuer, CardConnectionCreateRequest request) {
        if (ORGANIZATION_HYUNDAI.equals(cardIssuer.getOrganization())
                && (!StringUtils.hasText(request.cardNo()) || !StringUtils.hasText(request.cardPassword()))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /** 항상 예외를 던진다. */
    private void handleFailure(Long memberId, CardIssuer cardIssuer, CardConnection existing,
                                CodefAccountResult result, LocalDateTime now) {
        if (USER_ERROR_LIMIT_IMMINENT.equals(result.userErrorCode())) {
            if (ORGANIZATION_WOORI.equals(cardIssuer.getOrganization())) {
                // 우리카드는 하드락이 아니라 본인확인용 birthDate 추가입력으로 연결을 재개한다(§5-3).
                throw new BusinessException(ErrorCode.CARD_CONNECTION_BIRTHDATE_REQUIRED);
            }
            if (existing != null) {
                cardConnectionLockService.markLocked(existing.getId());
            }
            throw new BusinessException(ErrorCode.CARD_CONNECTION_LOCKED);
        }

        if (RESULT_CODE_PASSWORD_INVALID.equals(result.resultCode())) {
            boolean coolingDown = connectionAttemptService.recordFailure(memberId, cardIssuer, now);
            throw new BusinessException(coolingDown
                    ? ErrorCode.CARD_CONNECTION_COOLDOWN
                    : ErrorCode.CARD_CONNECTION_LOGIN_FAILED);
        }

        throw new BusinessException(ErrorCode.CARD_CONNECTION_LOGIN_FAILED);
    }
}
