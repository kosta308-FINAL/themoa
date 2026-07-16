package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.cardtransaction.service.ExchangeRateUnavailableException;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.fixedexpense.service.ConvertedKrwAmount;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseKrwConverter;
import com.weaone.themoa.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 급여 주기 예산 스냅샷의 생성·조회(dailyBudget.md §1). 주기 생성 시 해외 고정지출의 원화 스냅샷을 최신
 * 환율로 갱신한 뒤 {@code expected_amount_krw} 합을 {@code budget.expected_fixed_expense_total}에 굳힌다 —
 * 주기 도중 환율이 움직여도 그 주기 예산은 안 바뀐다.
 *
 * <p>별도 "주기 시작 새벽 배치" 대신 소비 가이드 진입(setup/summary) 시점에 현재 주기 예산을 없으면
 * 만드는 지연 생성 방식이다. {@code cycle_start_date}·{@code cycle_end_date}는 생성 시점 {@code payday}로만
 * 계산해 저장하고 이후 UPDATE하지 않으므로, 지연 생성이어도 과거 주기 집계가 흔들리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetCycleService {

    private static final String CURRENCY_KRW = "KRW";

    private final BudgetRepository budgetRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpensePaymentRepository fixedExpensePaymentRepository;
    private final FixedExpenseKrwConverter krwConverter;

    /** 현재 주기 예산을 조회하고 없으면 생성한다. 호출 전 {@code member.hasSpendingGuideSetup()}이 참이어야 한다. */
    public Budget getOrCreateCurrentBudget(Member member, LocalDate today) {
        BudgetCyclePolicy.BudgetCycle cycle = BudgetCyclePolicy.cycleOf(member.getPayday(), today);
        return budgetRepository.findByMember_IdAndYearMonth(member.getId(), cycle.yearMonth())
                .orElseGet(() -> createCycle(member, cycle, today));
    }

    private Budget createCycle(Member member, BudgetCyclePolicy.BudgetCycle cycle, LocalDate today) {
        BigDecimal expectedFixedTotal = refreshAndSumFixedExpenses(member.getId(), today);
        BigDecimal confirmedTotal = fixedExpensePaymentRepository.sumPaidAmount(member.getId(), cycle.yearMonth());
        Budget budget = Budget.openCycle(member, cycle.yearMonth(), cycle.cycleStartDate(), cycle.cycleEndDate(),
                member.getSalaryAmount(), member.getSavingsTargetOrZero(), expectedFixedTotal, confirmedTotal);
        return budgetRepository.save(budget);
    }

    /**
     * 활성 고정지출의 원화 스냅샷을 갱신(해외만)한 뒤 합산한다. 예산 차감은 반드시 원화
     * {@code expected_amount_krw}로 한다 — 통화가 섞인 {@code expected_amount}를 합치면 500,000원 + $22 =
     * 500,022처럼 단위가 다른 수를 더한 값이 된다(dailyBudget.md §1).
     */
    private BigDecimal refreshAndSumFixedExpenses(Long memberId, LocalDate today) {
        List<FixedExpense> activeFixedExpenses =
                fixedExpenseRepository.findByMember_IdAndStatus(memberId, FixedExpenseStatus.ACTIVE);
        BigDecimal total = BigDecimal.ZERO;
        for (FixedExpense fixedExpense : activeFixedExpenses) {
            refreshOverseasKrw(fixedExpense, today);
            total = total.add(fixedExpense.getExpectedAmountKrw());
        }
        return total;
    }

    private void refreshOverseasKrw(FixedExpense fixedExpense, LocalDate today) {
        if (CURRENCY_KRW.equals(fixedExpense.getExpectedCurrency())) {
            return;
        }
        try {
            ConvertedKrwAmount converted =
                    krwConverter.convert(fixedExpense.getExpectedAmount(), fixedExpense.getExpectedCurrency(), today);
            fixedExpense.refreshKrwSnapshot(converted.krwAmount(), converted.convertedDate(), converted.exchangeRate());
        } catch (ExchangeRateUnavailableException e) {
            // 환율을 못 구하면 등록 시점 원화 스냅샷을 유지한다 — 예산 생성 자체를 막지 않는다(dailyBudget.md §1).
            log.warn("주기 시작 환율 갱신 실패, 기존 원화 스냅샷을 유지합니다. fixedExpenseId={}", fixedExpense.getId(), e);
        }
    }
}
