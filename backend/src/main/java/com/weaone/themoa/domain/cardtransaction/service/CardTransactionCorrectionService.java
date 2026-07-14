package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 거래 건별 사용자 정정 3종(cardtransaction.md §3-4, §4, category.md §2-④). 모두 본인 거래에만 적용되고,
 * 정정 후에는 각 도메인의 재수집 갱신 로직이 이 값을 덮어쓰지 않는다(엔티티 쪽 가드).
 */
@Service
@RequiredArgsConstructor
public class CardTransactionCorrectionService {

    private final CardTransactionRepository cardTransactionRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public void correctCategory(Long memberId, Long transactionId, Long categoryId) {
        CardTransaction transaction = getOwnedTransaction(memberId, transactionId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        transaction.correctCategory(category);
    }

    @Transactional
    public void correctCanceledAmount(Long memberId, Long transactionId, BigDecimal canceledAmount) {
        CardTransaction transaction = getOwnedTransaction(memberId, transactionId);
        if (!transaction.isCancelAmountUncertain()) {
            throw new BusinessException(ErrorCode.CARD_TRANSACTION_CANCEL_AMOUNT_NOT_CORRECTABLE);
        }
        transaction.correctCanceledAmount(canceledAmount);
    }

    @Transactional
    public void correctAmount(Long memberId, Long transactionId, BigDecimal amount) {
        CardTransaction transaction = getOwnedTransaction(memberId, transactionId);
        if (transaction.getOriginalAmount() == null) {
            throw new BusinessException(ErrorCode.CARD_TRANSACTION_AMOUNT_NOT_CORRECTABLE);
        }
        transaction.correctAmount(amount);
    }

    private CardTransaction getOwnedTransaction(Long memberId, Long transactionId) {
        return cardTransactionRepository.findByIdAndMember_Id(transactionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TRANSACTION_NOT_FOUND));
    }
}
