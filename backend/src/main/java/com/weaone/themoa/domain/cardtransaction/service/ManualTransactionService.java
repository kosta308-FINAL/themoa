package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.cardtransaction.dto.request.ManualTransactionCreateRequest;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 수기 입력(entryMode.md §5) 생성의 단일 진입점. 가맹점 신원·업종이 없어 카테고리 자동 분류 파이프라인을
 * 우회하고 사용자가 지정한 카테고리를 그대로 확정한다(§5-1의 카드 결제수단 제한은 여기서 재검증한다).
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
        if (request.paymentMethod() == PaymentMethod.CARD && !member.isManualCardEntryAllowed()) {
            throw new BusinessException(ErrorCode.MANUAL_CARD_ENTRY_NOT_ALLOWED);
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        LocalDateTime usedAt = request.usedAt() != null ? request.usedAt() : request.usedDate().atStartOfDay();
        CardTransaction transaction = CardTransaction.manual(member, category, request.paymentMethod(),
                request.usedDate(), usedAt, request.amount(), request.description(), request.memo());
        cardTransactionRepository.save(transaction);
        return CardTransactionResponse.from(transaction);
    }
}
