package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.cardtransaction.dto.request.ManualTransactionCreateRequest;
import com.weaone.themoa.domain.cardtransaction.dto.request.ManualTransactionUpdateRequest;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionSource;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 수기 입력(entryMode.md §5, dayguide.md §4.2·§8.1)의 생성·수정·삭제. 가맹점 신원·업종이 없어 카테고리
 * 자동 분류 파이프라인을 우회하고 사용자가 지정한 카테고리를 그대로 확정한다(§5-1의 카드 결제수단 제한은
 * 생성·수정 모두에서 재검증한다).
 */
@Service
@RequiredArgsConstructor
public class ManualTransactionService {

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final CardTransactionRepository cardTransactionRepository;

    @Transactional
    public CardTransactionResponse create(Long memberId, ManualTransactionCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        validateCardEntryAllowed(member, request.paymentMethod());
        LocalDateTime usedAt = combine(request.usedDate(), request.usedTime());
        rejectFutureUsedAt(usedAt);
        Category category = getCategory(request.categoryId());

        CardTransaction transaction = CardTransaction.manual(member, category, request.paymentMethod(),
                request.usedDate(), usedAt, request.amount(), request.merchantName(), request.memo());
        cardTransactionRepository.save(transaction);
        return CardTransactionResponse.from(transaction);
    }

    /** 본인 소유의 {@code source=MANUAL} 거래만 허용한다(dayguide.md §8.1). */
    @Transactional
    public CardTransactionResponse update(Long memberId, Long transactionId, ManualTransactionUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        CardTransaction transaction = getOwnedManualTransaction(memberId, transactionId);
        validateCardEntryAllowed(member, request.paymentMethod());
        LocalDateTime usedAt = combine(request.usedDate(), request.usedTime());
        rejectFutureUsedAt(usedAt);
        Category category = getCategory(request.categoryId());

        transaction.updateManual(category, request.paymentMethod(), request.usedDate(), usedAt,
                request.amount(), request.merchantName(), request.memo());
        return CardTransactionResponse.from(transaction);
    }

    /** 대체 보존된(replaced_at 존재) 수기행은 조회 자체에서 빠지므로(엔티티의 {@code @SQLRestriction}) 삭제 대상이 될 수 없다. */
    @Transactional
    public void delete(Long memberId, Long transactionId) {
        CardTransaction transaction = getOwnedManualTransaction(memberId, transactionId);
        cardTransactionRepository.delete(transaction);
    }

    private CardTransaction getOwnedManualTransaction(Long memberId, Long transactionId) {
        return cardTransactionRepository.findByIdAndMember_IdAndSource(transactionId, memberId, TransactionSource.MANUAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TRANSACTION_NOT_FOUND));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateCardEntryAllowed(Member member, PaymentMethod paymentMethod) {
        if (paymentMethod == PaymentMethod.CARD && !member.isManualCardEntryAllowed()) {
            throw new BusinessException(ErrorCode.MANUAL_CARD_ENTRY_NOT_ALLOWED);
        }
    }

    private LocalDateTime combine(LocalDate usedDate, LocalTime usedTime) {
        return usedDate.atTime(usedTime != null ? usedTime : LocalTime.MIDNIGHT);
    }

    /** 신규 입력 기본값은 현재 시각이며 과거로는 자유롭게 변경 가능하지만 미래 시각은 저장할 수 없다(dayguide.md §4.2). */
    private void rejectFutureUsedAt(LocalDateTime usedAt) {
        if (usedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
