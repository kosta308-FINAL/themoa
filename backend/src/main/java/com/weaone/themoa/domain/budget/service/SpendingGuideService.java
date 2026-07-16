package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.dto.request.SalaryUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SavingsGoalUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SpendingGuideSetupRequest;
import com.weaone.themoa.domain.budget.dto.response.SpendingGuideSummaryResponse;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 소비 가이드 예산 유스케이스(dailyBudget.md, dayguide.md §8.1 BUD 범위): 최초 설정 · 오늘 현황 요약 ·
 * 월급/저축 목표 변경. 월 예산·하루 권장액은 저장하지 않고 {@code budget} 스냅샷 + 오늘 날짜 + 순지출로
 * 매 조회 시 계산한다.
 */
@Service
@RequiredArgsConstructor
public class SpendingGuideService {

    private static final String FIELD_SALARY = "salaryAmount";
    private static final String FIELD_PAYDAY = "payday";

    private final MemberRepository memberRepository;
    private final BudgetCycleService budgetCycleService;
    private final CardTransactionRepository cardTransactionRepository;

    /** S-00A 최초 설정. 월급·급여일을 저장하고 현재 주기 예산을 없으면 생성한 뒤 요약을 돌려준다. */
    @Transactional
    public SpendingGuideSummaryResponse setup(Long memberId, SpendingGuideSetupRequest request) {
        Member member = getMember(memberId);
        member.configureSpendingGuide(request.salaryAmount(), request.payday());
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        Budget budget = budgetCycleService.getOrCreateCurrentBudget(member, today);
        return buildSummary(memberId, budget, today);
    }

    /** S-01 오늘 현황·예산 기준. 미등록이면 setupRequired로 반환한다. */
    @Transactional
    public SpendingGuideSummaryResponse getSummary(Long memberId) {
        Member member = getMember(memberId);
        if (!member.hasSpendingGuideSetup()) {
            return SpendingGuideSummaryResponse.setupRequired(missingFields(member));
        }
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        Budget budget = budgetCycleService.getOrCreateCurrentBudget(member, today);
        return buildSummary(memberId, budget, today);
    }

    /**
     * 월급 수정(MOA-S-BUD-BGT-08). 원본({@code member.salary_amount})은 어느 쪽이든 즉시 갱신하고,
     * CURRENT_CYCLE일 때만 현재 주기 스냅샷을 덮어써 월 예산·하루 권장액이 즉시 따라오게 한다.
     */
    @Transactional
    public void changeSalary(Long memberId, SalaryUpdateRequest request) {
        Member member = getMember(memberId);
        member.changeSalary(request.amount());
        if (request.applyFrom() == BudgetApplyScope.CURRENT_CYCLE && member.hasSpendingGuideSetup()) {
            LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
            budgetCycleService.getOrCreateCurrentBudget(member, today).applySalary(request.amount());
        }
    }

    /** 저축 목표 설정·수정(MOA-S-BUD-BGT-03). 규칙은 월급과 동일. */
    @Transactional
    public void changeSavingsGoal(Long memberId, SavingsGoalUpdateRequest request) {
        Member member = getMember(memberId);
        member.changeSavingsTarget(request.amount());
        if (request.applyFrom() == BudgetApplyScope.CURRENT_CYCLE && member.hasSpendingGuideSetup()) {
            LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
            budgetCycleService.getOrCreateCurrentBudget(member, today).applySavingsGoal(request.amount());
        }
    }

    private SpendingGuideSummaryResponse buildSummary(Long memberId, Budget budget, LocalDate today) {
        LocalDate cycleStart = budget.getCycleStartDate();
        LocalDate cycleEnd = budget.getCycleEndDate();
        int remainingDays = BudgetCyclePolicy.remainingDays(today, cycleEnd);

        BigDecimal spentThroughYesterday = netSpend(memberId, cycleStart, today.minusDays(1));
        BigDecimal todayNetSpend = netSpend(memberId, today, today);
        BigDecimal spentThisCycle = netSpend(memberId, cycleStart, today);

        BigDecimal available = budget.getAvailableAmount();
        BigDecimal daily = budget.getDailyRecommendedAmount(spentThroughYesterday, remainingDays);
        // 오늘 사용 가능 = max(0, 하루 권장액 − max(오늘 순사용액, 0)). Type 2 취소로 순사용액이 음수여도 권장액을 넘기지 않는다.
        BigDecimal todayAvailable = daily.subtract(todayNetSpend.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
        BigDecimal remaining = available.subtract(spentThisCycle);

        boolean overCycleBudget = remaining.signum() < 0;
        BigDecimal cycleOverspent = overCycleBudget ? remaining.negate() : BigDecimal.ZERO;
        boolean budgetUnaffordable = available.signum() < 0;

        return SpendingGuideSummaryResponse.ready(budget.getYearMonth(), cycleStart, cycleEnd, remainingDays,
                budget.getSalaryAmount(), budget.getSavingsGoalAmount(), budget.getExpectedFixedExpenseTotal(),
                available, daily, todayNetSpend, todayAvailable, remaining, overCycleBudget, cycleOverspent,
                budgetUnaffordable);
    }

    /** 고정지출 태그·거절 제외 순지출 합계. 범위가 비면(주기 첫날의 "어제까지") 0. */
    private BigDecimal netSpend(Long memberId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }
        return cardTransactionRepository.sumNetSpend(memberId, TransactionStatus.REJECTED, startDate, endDate);
    }

    private List<String> missingFields(Member member) {
        List<String> missing = new ArrayList<>();
        if (member.getSalaryAmount() == null) {
            missing.add(FIELD_SALARY);
        }
        if (member.getPayday() == null) {
            missing.add(FIELD_PAYDAY);
        }
        return missing;
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
