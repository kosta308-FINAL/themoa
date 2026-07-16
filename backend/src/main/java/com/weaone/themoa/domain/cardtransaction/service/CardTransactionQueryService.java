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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardTransactionQueryService {

    private final CardTransactionRepository cardTransactionRepository;

    @Transactional(readOnly = true)
    public Page<CardTransactionResponse> list(Long memberId, Pageable pageable) {
        return cardTransactionRepository.findByMember_IdOrderByUsedAtDesc(memberId, pageable)
                .map(CardTransactionResponse::from);
    }

    /**
     * 카테고리별 소비 비중/내역(category.md §6·§7). 거절·고정지출 태그 거래 제외, 도넛은 순액이
     * 0원보다 큰 소비만 포함(Type 2 음수행 포함), canceledTotal은 Type 1 취소금액 + Type 2 음수행
     * 절대값 합이다.
     */
    @Transactional(readOnly = true)
    public CategorySummaryListResponse summarizeByCategory(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<CardTransactionRepository.CategorySummary> summaries = cardTransactionRepository
                .summarizeByCategory(memberId, TransactionStatus.REJECTED, startDate, endDate);
        BigDecimal canceledTotal = cardTransactionRepository
                .sumCanceledAmount(memberId, TransactionStatus.REJECTED, startDate, endDate);
        return CategorySummaryListResponse.from(summaries, canceledTotal);
    }
}
