package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
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
}
