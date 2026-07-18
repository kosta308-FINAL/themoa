package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.budget.service.SpendingGuideService;
import com.weaone.themoa.domain.coaching.dto.response.CoachingCardListResponse;
import com.weaone.themoa.domain.coaching.dto.response.CoachingCardResponse;
import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import com.weaone.themoa.domain.coaching.repository.CoachingCardRepository;
import com.weaone.themoa.domain.member.entity.EntryMode;
import com.weaone.themoa.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 소비 가이드 화면 코칭 카드 조회(habitExpense.md §5, dayguide.md §3.5·§8.1). {@code budgetId} 생략 시
 * 현재 급여 주기를 기준으로, 그 주기 동안 화면에 표시되는 카드(=직전 완료 주기를 분석해 만든 카드)를
 * 반환한다. 카드가 없으면 오류가 아니라 {@code emptyReason}으로 사유를 구분한다.
 */
@Service
@RequiredArgsConstructor
public class CoachingCardQueryService {

    private static final String EMPTY_REASON_CARD_NOT_CONNECTED = "CARD_NOT_CONNECTED";
    private static final String EMPTY_REASON_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    private final CoachingCardRepository coachingCardRepository;
    private final SpendingGuideService spendingGuideService;

    @Transactional(readOnly = true)
    public CoachingCardListResponse list(Long memberId, Long budgetId) {
        Budget budget = spendingGuideService.getBudgetOrCurrent(memberId, budgetId);
        Member member = budget.getMember();
        String analyzedYearMonth = HabitCoachingCandidateExtractionService
                .previousCompletedCycle(member.getPayday(), budget.getCycleStartDate())
                .yearMonth();

        List<CoachingCardResponse> items = coachingCardRepository
                .findByMember_IdAndYearMonthOrderByDisplayOrderAsc(memberId, analyzedYearMonth).stream()
                .map(CoachingCardResponse::from)
                .toList();
        String emptyReason = items.isEmpty() ? resolveEmptyReason(member) : null;
        return new CoachingCardListResponse(items, emptyReason);
    }

    private String resolveEmptyReason(Member member) {
        return member.getEntryMode() == EntryMode.MANUAL
                ? EMPTY_REASON_CARD_NOT_CONNECTED
                : EMPTY_REASON_INSUFFICIENT_DATA;
    }
}
