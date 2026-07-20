package com.weaone.themoa.domain.cardconnection.service;

import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드사 계정 잠금(userError=99, connection.md §5-2) 반영 전용.
 * 호출자가 곧바로 예외를 던져 트랜잭션을 롤백시키므로, 잠금 표시는 별도 트랜잭션으로 커밋한다.
 */
@Service
@RequiredArgsConstructor
public class CardConnectionLockService {

    private final CardConnectionRepository cardConnectionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markLocked(Long cardConnectionId) {
        cardConnectionRepository.findById(cardConnectionId).ifPresent(CardConnection::markLocked);
    }
}
