package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.entity.PaydayChangeHistory;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.repository.PaydayChangeHistoryRepository;
import com.weaone.themoa.domain.cardtransaction.service.ExchangeRateUnavailableException;
import com.weaone.themoa.domain.cardtransaction.support.BackfillWindowPolicy;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.fixedexpense.service.ConvertedKrwAmount;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseKrwConverter;
import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.repository.MemberWorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final MemberWorkScheduleRepository memberWorkScheduleRepository;
    private final WorkScheduleSalaryCalculator workScheduleSalaryCalculator;
    private final PaydayChangeHistoryRepository paydayChangeHistoryRepository;
    private final MemberRepository memberRepository;

    /** 현재 주기 예산을 조회하고 없으면 생성한다. 호출 전 {@code member.hasSpendingGuideSetup()}이 참이어야 한다. */
    public Budget getOrCreateCurrentBudget(Member member, LocalDate today) {
        Optional<BudgetCyclePolicy.BudgetCycle> bridge = ensurePaydayPromoted(member, today);
        BudgetCyclePolicy.BudgetCycle cycle = bridge
                .filter(b -> !today.isAfter(b.cycleEndDate()))
                .orElseGet(() -> BudgetCyclePolicy.cycleOf(member.getPayday(), today));
        return getOrCreateCycle(member, cycle, today);
    }

    /**
     * 급여일 변경 예약(payday.md §급여일 변경) 승격 지점. "다음 주기"는 기존 payday로 계산한 오늘의 주기에
     * 해당하는 {@code Budget} row가 아직 없는 시점이다 — 그 전까지는 진행 중인 주기를 절대 건드리지 않는다
     * (dailyBudget.md §1 "과거·현재 주기 불변" 원칙과 같은 메커니즘을 재사용). 승격 시 이력을 남기고, 지금
     * 막 새로 열리는 주기가 브리지 주기이면 그 범위를 돌려준다(호출자가 바로 써서 Budget을 만들 수 있게).
     * pending 예약이 없거나 아직 승격할 시점이 아니면 아무 것도 하지 않고 빈 값을 돌려준다.
     */
    @Transactional
    public Optional<BudgetCyclePolicy.BudgetCycle> ensurePaydayPromoted(Member member, LocalDate today) {
        if (member.getPendingPayday() == null) {
            return Optional.empty();
        }
        BudgetCyclePolicy.BudgetCycle openCycle = BudgetCyclePolicy.cycleOf(member.getPayday(), today);
        if (budgetRepository.findByMember_IdAndYearMonth(member.getId(), openCycle.yearMonth()).isPresent()) {
            return Optional.empty();
        }
        LocalDate previousCycleEnd = budgetRepository.findFirstByMember_IdOrderByCycleStartDateDesc(member.getId())
                .map(Budget::getCycleEndDate)
                .orElse(today.minusDays(1));
        int oldPayday = member.getPayday();
        int newPayday = member.getPendingPayday();
        BudgetCyclePolicy.BudgetCycle bridge = BudgetCyclePolicy.bridgeCycle(previousCycleEnd, newPayday);
        paydayChangeHistoryRepository.save(
                PaydayChangeHistory.record(member, oldPayday, newPayday, bridge.cycleStartDate(), LocalDateTime.now()));
        member.applyPendingPayday();
        return Optional.of(bridge);
    }

    /** {@link #ensurePaydayPromoted(Member, LocalDate)}의 memberId 진입점. 배치 루프처럼 준영속 상태로
     * member를 들고 있는 호출자를 위한 것 — 관리 상태 엔티티를 새로 조회해 안전하게 승격한다. */
    @Transactional
    public void ensurePaydayPromoted(Long memberId, LocalDate today) {
        memberRepository.findById(memberId).ifPresent(member -> ensurePaydayPromoted(member, today));
    }

    /**
     * 임의 날짜(주로 과거)가 속한 주기를 급여일 변경 이력까지 반영해 계산한다. {@code Budget} row 존재
     * 여부와 무관하게 동작하도록 만든 순수 조회 경로다 — 고정지출 매칭처럼 그 시점 {@code Budget}이 아직
     * 생성돼 있지 않을 수 있는 도메인에서 쓴다. 아직 승격되지 않은 예약(pendingPayday)은 반영하지 않는다 —
     * "오늘" 기준 최신 상태가 필요하면 먼저 {@link #ensurePaydayPromoted(Member, LocalDate)}를 호출해야 한다.
     */
    @Transactional(readOnly = true)
    public BudgetCyclePolicy.BudgetCycle resolveCycleForDate(Member member, LocalDate date) {
        List<PaydayChangeHistory> history =
                paydayChangeHistoryRepository.findByMember_IdOrderByEffectiveCycleStartDateAsc(member.getId());
        PaydayChangeHistory applicable = null;
        for (PaydayChangeHistory h : history) {
            if (!h.getEffectiveCycleStartDate().isAfter(date)) {
                applicable = h;
            } else {
                break;
            }
        }
        if (applicable == null) {
            int paydayThen = history.isEmpty() ? member.getPayday() : history.get(0).getOldPayday();
            return BudgetCyclePolicy.cycleOf(paydayThen, date);
        }
        BudgetCyclePolicy.BudgetCycle bridge = BudgetCyclePolicy.bridgeCycle(
                applicable.getEffectiveCycleStartDate().minusDays(1), applicable.getNewPayday());
        return date.isAfter(bridge.cycleEndDate())
                ? BudgetCyclePolicy.cycleOf(applicable.getNewPayday(), date)
                : bridge;
    }

    /**
     * 기준일이 속한 주기 바로 이전, 완료된 주기(습관 코칭 §3·§8, 코칭 카드 조회 공용). 급여일 변경 이력을
     * 반영해 정확한 경계를 계산한다. referenceDate가 "오늘"이면 먼저 승격을 시도해 최신 payday를 보장한다.
     */
    @Transactional
    public BudgetCyclePolicy.BudgetCycle previousCompletedCycle(Member member, LocalDate referenceDate) {
        ensurePaydayPromoted(member, referenceDate);
        BudgetCyclePolicy.BudgetCycle current = resolveCycleForDate(member, referenceDate);
        return resolveCycleForDate(member, current.cycleStartDate().minusDays(1));
    }

    /**
     * 최초 카드 백필 완료 시(또는 그 이후 소비 가이드 최초 설정 시) 과거 급여주기의 budget row를 소급
     * 생성한다 — {@code card_transaction}은 최초 연동 시 과거 3개월치가 한번에 채워지는데 {@code budget}은
     * 지연 생성이라 최초 사용자는 이전 주기 조회가 항상 막히는 문제를 해소한다. 카드 백필 상한과 같은
     * {@link BackfillWindowPolicy}를 사용해 두 데이터의 소급 범위를 맞춘다. 과거 실제 월급·저축목표는 알 수
     * 없으므로 소비 가이드 최초 설정 시점의 값을 그대로 각 과거 주기에 적용한다(근사치). 호출 전
     * {@code member.hasSpendingGuideSetup()}이 참이어야 한다.
     */
    @Transactional
    public void backfillPastCycles(Member member, LocalDate today) {
        BudgetCyclePolicy.BudgetCycle currentCycle = BudgetCyclePolicy.cycleOf(member.getPayday(), today);
        LocalDate windowStart = BackfillWindowPolicy.calendarFloor(today);
        BudgetCyclePolicy.BudgetCycle cursor = BudgetCyclePolicy.cycleOf(member.getPayday(), windowStart);
        while (cursor.cycleStartDate().isBefore(currentCycle.cycleStartDate())) {
            getOrCreateCycle(member, cursor, today);
            cursor = BudgetCyclePolicy.cycleOf(member.getPayday(), cursor.cycleEndDate().plusDays(1));
        }
    }

    private Budget getOrCreateCycle(Member member, BudgetCyclePolicy.BudgetCycle cycle, LocalDate today) {
        return budgetRepository.findByMember_IdAndYearMonth(member.getId(), cycle.yearMonth())
                .orElseGet(() -> createCycle(member, cycle, today));
    }

    private Budget createCycle(Member member, BudgetCyclePolicy.BudgetCycle cycle, LocalDate today) {
        BigDecimal expectedFixedTotal = refreshAndSumFixedExpenses(member.getId(), today);
        BigDecimal confirmedTotal = fixedExpensePaymentRepository.sumPaidAmount(member.getId(), cycle.yearMonth());
        BigDecimal salaryAmount = resolveSalaryAmount(member, cycle);
        Budget budget = Budget.openCycle(member, cycle.yearMonth(), cycle.cycleStartDate(), cycle.cycleEndDate(),
                salaryAmount, member.getSavingsTargetOrZero(), expectedFixedTotal, confirmedTotal);
        return budgetRepository.save(budget);
    }

    /** SALARY는 원본을 그대로, HOURLY는 그 주기 날짜 범위에 요일별 스케줄을 대입해 예상 소득을 산출한다. */
    private BigDecimal resolveSalaryAmount(Member member, BudgetCyclePolicy.BudgetCycle cycle) {
        if (member.getIncomeType() != IncomeType.HOURLY) {
            return member.getSalaryAmount();
        }
        return workScheduleSalaryCalculator.calculate(
                memberWorkScheduleRepository.findByMember_Id(member.getId()), member.getHourlyWage(),
                cycle.cycleStartDate(), cycle.cycleEndDate());
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
