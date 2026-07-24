package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * F-01/F-04 상태 배지 계산(view/fixedExpense.md §4). 연동 여부·결제수단과 무관하게 결제일 윈도우로 계산한다 —
 * PAID는 카드 자동매칭(연동형) 또는 사용자의 수동 결제처리(미연동·이체형, {@link FixedExpenseConfirmationService#confirmManually})
 * 어느 쪽으로 만들어진 이행 기록이든 동일하게 인정한다. {@link FixedExpenseNotificationBatchService}가 쓰는
 * 미납·예정일 판정과 같은 기준(±3일 유예)을 그대로 따른다 — 여기는 조회 시점 계산이고, 그쪽은 알림 생성 배치라는 차이만 있다.
 */
@Service
@RequiredArgsConstructor
public class FixedExpensePaymentStatusService {

    private static final long DUE_SOON_WINDOW_DAYS = 3;
    private static final long MISSED_GRACE_DAYS = 3;

    private final FixedExpensePaymentRepository fixedExpensePaymentRepository;
    private final MemberRepository memberRepository;
    private final BudgetCycleService budgetCycleService;

    /**
     * fixedExpense는 다른(이미 닫힌) 트랜잭션에서 조회된 detached 엔티티일 수 있어 {@code fixedExpense.getMember()}의
     * 지연 로딩 연관을 그대로 타면 LazyInitializationException이 난다. member.getId()는 프록시에 저장된 값이라
     * 안전하지만 payday는 이 메서드 자신의 세션에서 별도 조회해야 한다. 조회 전용(readOnly)이라 여기서
     * 급여일 변경 예약을 승격하지는 않는다 — 이미 승격된 이력·payday까지만 반영한다.
     */
    @Transactional(readOnly = true)
    public String resolve(FixedExpense fixedExpense) {
        if (fixedExpense.getExpectedPayDay() == null) {
            return null;
        }
        Long memberId = fixedExpense.getMember().getId();
        Member member = memberRepository.getReferenceById(memberId);
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        String yearMonth = member.getPayday() == null
                ? FixedExpenseCyclePolicy.currentYearMonth(null)
                : budgetCycleService.resolveCycleForDate(member, today).yearMonth();
        if (fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(fixedExpense.getId(), yearMonth)) {
            return "PAID";
        }

        LocalDate expectedDate = expectedDateInMonth(fixedExpense, today);
        if (today.isAfter(expectedDate.plusDays(MISSED_GRACE_DAYS))) {
            return "MISSED";
        }
        if (!expectedDate.isBefore(today) && !expectedDate.isAfter(today.plusDays(DUE_SOON_WINDOW_DAYS))) {
            return "DUE_SOON";
        }
        return null;
    }

    /** 없는 날짜(31일 등)는 그 달 말일로 당긴다(erd.md member.payday와 동일한 관례). */
    private LocalDate expectedDateInMonth(FixedExpense fixedExpense, LocalDate today) {
        int day = Math.min(fixedExpense.getExpectedPayDay(), today.lengthOfMonth());
        return today.withDayOfMonth(day);
    }
}
