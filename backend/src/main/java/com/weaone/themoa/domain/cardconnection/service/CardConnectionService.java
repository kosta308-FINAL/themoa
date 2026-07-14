package com.weaone.themoa.domain.cardconnection.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.cardconnection.client.CodefAccountResult;
import com.weaone.themoa.domain.cardconnection.client.CodefCardConnectionClient;
import com.weaone.themoa.domain.cardconnection.client.CodefClientException;
import com.weaone.themoa.domain.cardconnection.client.CodefCreateAccountCommand;
import com.weaone.themoa.domain.cardconnection.dto.request.CardConnectionCreateRequest;
import com.weaone.themoa.domain.cardconnection.dto.response.CardConnectionResponse;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardconnection.repository.ConnectionAttemptRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 카드사 커넥션(connectedId) 등록. 결제내역 수집 자체는 cardtransaction.md 소관이라 여기서 다루지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardConnectionService {

    /** connection.md §3-1 — ID 로그인 파라미터상 카드번호+카드비밀번호가 필수인 카드사는 현대뿐이다. */
    private static final String ORGANIZATION_HYUNDAI = "0302";

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
        CardConnection connection = CardConnection.connect(member, cardIssuer, result.connectedId(), now);
        cardConnectionRepository.save(connection);
        return CardConnectionResponse.from(connection);
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
