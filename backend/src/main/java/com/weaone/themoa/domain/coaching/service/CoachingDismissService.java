package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import com.weaone.themoa.domain.coaching.entity.CoachingCardTargetType;
import com.weaone.themoa.domain.coaching.entity.CoachingDismiss;
import com.weaone.themoa.domain.coaching.entity.CoachingDismissType;
import com.weaone.themoa.domain.coaching.repository.CoachingCardRepository;
import com.weaone.themoa.domain.coaching.repository.CoachingDismissRepository;
import com.weaone.themoa.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 코칭 카드 넘기기(habitExpense.md §5). 넘긴 카드 자신은 즉시 목록에서 빠지도록 dismissedAt을 남기고,
 * 카드가 이미 들고 있는 코칭 대상 참조(category/merchant_alias)는 그대로 써서 대상별 1행으로 upsert한다
 * — 이 upsert 결과는 다음 주기 후보 추출에 반영된다. 사용자가 나중에 다른 유형으로 다시 넘기면
 * (NOT_WASTE→HIDE 등) 기존 행을 갱신한다.
 */
@Service
@RequiredArgsConstructor
public class CoachingDismissService {

    private final CoachingCardRepository coachingCardRepository;
    private final CoachingDismissRepository coachingDismissRepository;

    @Transactional
    public void dismiss(Long memberId, Long cardId, CoachingDismissType dismissType) {
        CoachingCard card = coachingCardRepository.findByIdAndMember_Id(cardId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COACHING_CARD_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        card.markDismissed(now);
        Member member = card.getMember();
        if (card.getTargetType() == CoachingCardTargetType.CATEGORY) {
            upsertForCategory(member, card, dismissType, now);
        } else {
            upsertForMerchantAlias(member, card, dismissType, now);
        }
    }

    private void upsertForCategory(Member member, CoachingCard card, CoachingDismissType type, LocalDateTime now) {
        coachingDismissRepository.findByMember_IdAndCategory_Id(member.getId(), card.getCategory().getId())
                .ifPresentOrElse(
                        existing -> existing.updateDismissType(type, now),
                        () -> save(CoachingDismiss.forCategory(member, card.getCategory(), type, now)));
    }

    private void upsertForMerchantAlias(Member member, CoachingCard card, CoachingDismissType type, LocalDateTime now) {
        coachingDismissRepository.findByMember_IdAndMerchantAlias_Id(member.getId(), card.getMerchantAlias().getId())
                .ifPresentOrElse(
                        existing -> existing.updateDismissType(type, now),
                        () -> save(CoachingDismiss.forMerchantAlias(member, card.getMerchantAlias(), type, now)));
    }

    private void save(CoachingDismiss dismiss) {
        try {
            coachingDismissRepository.save(dismiss);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경합 — 이미 다른 요청이 같은 대상의 dismiss를 저장했다. 무시한다.
        }
    }
}
