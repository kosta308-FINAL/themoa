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

/** 고정지출 화면의 "연 환산" 코칭 카드 조회. 카드 생성은 매일 새벽 3시 배치가 담당하고, 여기서는 읽기만 한다. */
@Service
@RequiredArgsConstructor
public class FixedExpenseCoachingQueryService {

    private final MemberRepository memberRepository;
    private final FixedExpenseCoachingCardRepository coachingCardRepository;
    private final BudgetCycleService budgetCycleService;

    @Transactional(readOnly = true)
    public List<FixedExpenseCoachingCardResponse> list(Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        String yearMonth = member.getPayday() == null
                ? FixedExpenseCyclePolicy.currentYearMonth(null)
                : budgetCycleService.resolveCycleForDate(member, today).yearMonth();

        return coachingCardRepository
                .findByMember_IdAndYearMonthAndDismissedAtIsNullOrderByDisplayOrderAsc(memberId, yearMonth).stream()
                .map(FixedExpenseCoachingCardResponse::from)
                .toList();
    }
}
