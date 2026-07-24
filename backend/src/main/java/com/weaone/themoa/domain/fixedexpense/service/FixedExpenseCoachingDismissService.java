package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCoachingCard;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCoachingDismiss;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCoachingCardRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCoachingDismissRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 고정지출 코칭 카드 "다시 보지 않기". 이 카드는 즉시 목록에서 빠지고, 대상 고정지출은 다음 주기 후보
 * 추출({@link FixedExpenseCoachingCandidateExtractionService})에서 영구히 제외된다.
 */
@Service
@RequiredArgsConstructor
public class FixedExpenseCoachingDismissService {

    private final FixedExpenseCoachingCardRepository coachingCardRepository;
    private final FixedExpenseCoachingDismissRepository coachingDismissRepository;

    @Transactional
    public void dismiss(Long memberId, Long cardId) {
        FixedExpenseCoachingCard card = coachingCardRepository.findByIdAndMember_Id(cardId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_COACHING_CARD_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        card.markDismissed(now);

        if (coachingDismissRepository.existsByMember_IdAndFixedExpense_Id(memberId, card.getFixedExpense().getId())) {
            return;
        }
        try {
            coachingDismissRepository.save(
                    FixedExpenseCoachingDismiss.of(card.getMember(), card.getFixedExpense(), now));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경합 — 이미 다른 요청이 같은 대상의 dismiss를 저장했다. 무시한다.
        }
    }
}
