package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardTransactionQueryService {

    private final CardTransactionRepository cardTransactionRepository;

    @Transactional(readOnly = true)
    public Page<CardTransactionResponse> list(Long memberId, Pageable pageable) {
        return cardTransactionRepository.findByMember_IdOrderByUsedAtDesc(memberId, pageable)
                .map(CardTransactionResponse::from);
    }

    /** S-02 거래 상세(dayguide.md §8.1). 본인 소유가 아니면 존재를 숨기고 404. */
    @Transactional(readOnly = true)
    public CardTransactionResponse getDetail(Long memberId, Long transactionId) {
        CardTransaction transaction = cardTransactionRepository.findByIdAndMember_Id(transactionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TRANSACTION_NOT_FOUND));
        return CardTransactionResponse.from(transaction);
    }
}
