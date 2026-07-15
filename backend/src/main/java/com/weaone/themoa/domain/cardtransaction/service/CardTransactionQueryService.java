package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategorySummaryListResponse;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardTransactionQueryService {

    private static final List<TransactionStatus> AGGREGATABLE_STATUSES =
            List.of(TransactionStatus.APPROVED, TransactionStatus.PARTIAL_CANCELED);

    private final CardTransactionRepository cardTransactionRepository;

    @Transactional(readOnly = true)
    public Page<CardTransactionResponse> list(Long memberId, Pageable pageable) {
        return cardTransactionRepository.findByMember_IdOrderByUsedAtDesc(memberId, pageable)
                .map(CardTransactionResponse::from);
    }

    /** 카테고리별 소비 비중/내역(category.md §6·§7). 취소 제외, 부분취소는 순액으로 반영한다. */
    @Transactional(readOnly = true)
    public CategorySummaryListResponse summarizeByCategory(Long memberId, LocalDate startDate, LocalDate endDate) {
        return CategorySummaryListResponse.from(cardTransactionRepository
                .summarizeByCategory(memberId, AGGREGATABLE_STATUSES, startDate, endDate));
    }
}
