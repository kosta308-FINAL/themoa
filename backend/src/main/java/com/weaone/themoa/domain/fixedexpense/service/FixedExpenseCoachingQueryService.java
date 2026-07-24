package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.fixedexpense.dto.response.FixedExpenseCoachingCardResponse;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCoachingCardRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 고정지출 화면의 "연 환산" 코칭 카드 조회. 이번 주기 카드가 없으면 조회 시점에 만든다(지연 생성). */
@Service
@RequiredArgsConstructor
public class FixedExpenseCoachingQueryService {

    private final MemberRepository memberRepository;
    private final FixedExpenseCoachingCardRepository coachingCardRepository;
    private final FixedExpenseCoachingCardService coachingCardService;
    private final BudgetCycleService budgetCycleService;

    /** readOnly가 아니다 — 카드 생성(쓰기)이 조회 경로 안에서 일어날 수 있다. */
    @Transactional
    public List<FixedExpenseCoachingCardResponse> list(Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        String yearMonth = member.getPayday() == null
                ? FixedExpenseCyclePolicy.currentYearMonth(null)
                : budgetCycleService.resolveCycleForDate(member, today).yearMonth();

        coachingCardService.generateForMemberIfMissing(memberId, yearMonth);

        return coachingCardRepository
                .findByMember_IdAndYearMonthAndDismissedAtIsNullOrderByDisplayOrderAsc(memberId, yearMonth).stream()
                .map(FixedExpenseCoachingCardResponse::from)
                .toList();
    }
}
