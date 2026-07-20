package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.dto.request.SalaryUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SavingsGoalUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SpendingGuideSetupRequest;
import com.weaone.themoa.domain.budget.dto.response.RecentDaysResponse;
import com.weaone.themoa.domain.budget.dto.response.SpendingGuideSummaryResponse;
import com.weaone.themoa.domain.budget.dto.response.TodayTransactionsResponse;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.cardconnection.entity.InitialSyncStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionListResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategorySummaryListResponse;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.cardtransaction.support.BackfillWindowPolicy;
import com.weaone.themoa.domain.member.entity.EntryMode;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final int TODAY_TRANSACTIONS_DEFAULT_LIMIT = 5;
    private static final int TODAY_TRANSACTIONS_MAX_LIMIT = 8;
    private static final int RECENT_DAYS_DEFAULT = 7;
    private static final int RECENT_DAYS_MAX = 7;

    private final MemberRepository memberRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetCycleService budgetCycleService;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardConnectionRepository cardConnectionRepository;

    /**
     * S-00A 최초 설정. 월급·급여일을 저장하고 현재 주기 예산을 없으면 생성한 뒤 요약을 돌려준다.
     *
     * <p>카드 연동이 이 설정보다 먼저 끝나 있었다면({@code member.payday}가 없어 {@link
     * BudgetCycleBackfillListener}가 소급 생성을 건너뛴 경우) 여기서 과거 급여주기 budget row 소급 생성을
     * 다시 시도한다 — 최초 사용자가 설정 직후 바로 "이전 주기 조회"를 쓸 수 있게 한다.
     */
    @Transactional
    public SpendingGuideSummaryResponse setup(Long memberId, SpendingGuideSetupRequest request) {
        Member member = getMember(memberId);
        member.configureSpendingGuide(request.salaryAmount(), request.payday());
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        if (cardConnectionRepository.existsByMember_IdAndInitialSyncStatus(memberId, InitialSyncStatus.COMPLETED)) {
            budgetCycleService.backfillPastCycles(member, today);
        }
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

    /** S-01 오늘 거래 미리보기(dayguide.md §8.1). {@code limit}은 기본 5·최대 8로 clamp한다. */
    @Transactional(readOnly = true)
    public TodayTransactionsResponse getTodayTransactions(Long memberId, Integer limit) {
        Member member = getMemberWithSetup(memberId);
        int clampedLimit = clamp(limit, TODAY_TRANSACTIONS_DEFAULT_LIMIT, TODAY_TRANSACTIONS_MAX_LIMIT);
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        Page<CardTransactionResponse> page = cardTransactionRepository
                .findByMember_IdAndFixedExpenseIsNullAndStatusNotAndUsedDateOrderByUsedAtDesc(
                        member.getId(), TransactionStatus.REJECTED, today, PageRequest.of(0, clampedLimit))
                .map(CardTransactionResponse::from);
        return new TodayTransactionsResponse(page.getContent(), page.getTotalElements());
    }

    /** S-01 최근 N일 막대그래프(dayguide.md §3.3·§8.1). 거래가 없는 날짜는 0원으로 채운다. */
    @Transactional(readOnly = true)
    public RecentDaysResponse getRecentDays(Long memberId, Integer days) {
        Member member = getMemberWithSetup(memberId);
        int clampedDays = clamp(days, RECENT_DAYS_DEFAULT, RECENT_DAYS_MAX);
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        LocalDate start = today.minusDays(clampedDays - 1L);

        Map<LocalDate, BigDecimal> netByDate = cardTransactionRepository
                .sumNetSpendByDate(member.getId(), TransactionStatus.REJECTED, start, today).stream()
                .collect(Collectors.toMap(CardTransactionRepository.DailyNetAmount::getUsedDate,
                        CardTransactionRepository.DailyNetAmount::getNetAmount));
        List<RecentDaysResponse.DailyAmount> daily = start.datesUntil(today.plusDays(1))
                .map(date -> new RecentDaysResponse.DailyAmount(date, netByDate.getOrDefault(date, BigDecimal.ZERO)))
                .toList();

        Budget budget = budgetCycleService.getOrCreateCurrentBudget(member, today);
        int remainingDays = BudgetCyclePolicy.remainingDays(today, budget.getCycleEndDate());
        BigDecimal spentThroughYesterday = netSpend(memberId, budget.getCycleStartDate(), today.minusDays(1));
        BigDecimal guideLineAmount = budget.getDailyRecommendedAmount(spentThroughYesterday, remainingDays);
        return new RecentDaysResponse(daily, guideLineAmount);
    }

    /** S-04 전체 소비내역(dayguide.md §8.1). {@code budgetId} 생략 시 현재 주기를 조회한다. */
    @Transactional(readOnly = true)
    public CardTransactionListResponse searchTransactions(Long memberId, Long budgetId, LocalDate date,
                                                            Long categoryId, Pageable pageable) {
        Member member = getMemberWithSetup(memberId);
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        Budget budget = resolveBudget(member, budgetId, today);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "usedAt"));
        Page<CardTransactionResponse> page = cardTransactionRepository.searchForSpendingGuide(
                        member.getId(), TransactionStatus.REJECTED, budget.getCycleStartDate(), budget.getCycleEndDate(),
                        date, categoryId, sorted)
                .map(CardTransactionResponse::from);
        return CardTransactionListResponse.from(page);
    }

    /**
     * S-01 카테고리 도넛(dayguide.md §3.4·§8.1·§8.3). {@code budgetId} 생략 시 현재 주기를 조회하고,
     * 진행 중인 주기이거나 데이터가 일부만 있는 주기는 {@code completedCycleResult=null}이다.
     */
    @Transactional(readOnly = true)
    public CategorySummaryListResponse getCategorySummary(Long memberId, Long budgetId) {
        Member member = getMemberWithSetup(memberId);
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        Budget budget = resolveBudget(member, budgetId, today);

        LocalDate dataStartDate = resolveDataStartDate(member);
        LocalDate earliestCycleStart = BudgetCyclePolicy.cycleOf(member.getPayday(), dataStartDate).cycleStartDate();
        boolean partialCycle = budget.getCycleStartDate().isBefore(dataStartDate);
        boolean hasPrevious = budget.getCycleStartDate().isAfter(earliestCycleStart);
        boolean hasNext = today.isAfter(budget.getCycleEndDate());
        Long previousBudgetId = budgetRepository
                .findFirstByMember_IdAndCycleStartDateLessThanOrderByCycleStartDateDesc(memberId, budget.getCycleStartDate())
                .map(Budget::getId)
                .orElse(null);
        Long nextBudgetId = budgetRepository
                .findFirstByMember_IdAndCycleStartDateGreaterThanOrderByCycleStartDateAsc(memberId, budget.getCycleStartDate())
                .map(Budget::getId)
                .orElse(null);

        List<CardTransactionRepository.CategorySummary> summaries = cardTransactionRepository
                .summarizeByCategory(memberId, TransactionStatus.REJECTED, budget.getCycleStartDate(), budget.getCycleEndDate());
        BigDecimal canceledTotal = cardTransactionRepository
                .sumCanceledAmount(memberId, TransactionStatus.REJECTED, budget.getCycleStartDate(), budget.getCycleEndDate());
        CategorySummaryListResponse.CompletedCycleResult completedCycleResult =
                (hasNext && !partialCycle) ? completedCycleResult(memberId, budget) : null;

        return CategorySummaryListResponse.of(budget.getId(), budget.getYearMonth(), budget.getCycleStartDate(),
                budget.getCycleEndDate(), dataStartDate, partialCycle, hasPrevious, hasNext, previousBudgetId,
                nextBudgetId, summaries, canceledTotal, completedCycleResult);
    }

    /** 완료된 과거 주기의 예산·사용·결과 한 줄 요약(§3.4). 사용액은 고정지출 태그 거래를 제외한 순지출이다. */
    private CategorySummaryListResponse.CompletedCycleResult completedCycleResult(Long memberId, Budget budget) {
        BigDecimal budgetAmount = budget.getAvailableAmount();
        BigDecimal spentAmount = netSpend(memberId, budget.getCycleStartDate(), budget.getCycleEndDate());
        BigDecimal resultAmount = budgetAmount.subtract(spentAmount);
        String resultType = resultAmount.signum() >= 0 ? "REMAINED" : "EXCEEDED";
        return new CategorySummaryListResponse.CompletedCycleResult(budgetAmount, spentAmount, resultAmount, resultType);
    }

    /**
     * 카테고리 도넛 주기 이동 하한(§3.4): 카드 연동 사용자는 최초 3개월 백필의 시작일, 수기 모드 사용자는
     * 가입일이다.
     */
    private LocalDate resolveDataStartDate(Member member) {
        if (member.getEntryMode() == EntryMode.CARD && member.getCardSyncStartedAt() != null) {
            return BackfillWindowPolicy.calendarFloor(member.getCardSyncStartedAt().toLocalDate());
        }
        return member.getCreatedAt().toLocalDate();
    }

    /** 다른 도메인(코칭카드 등)이 budgetId 유무에 따른 주기 조회를 재사용할 수 있게 공개한 진입점. */
    @Transactional(readOnly = true)
    public Budget getBudgetOrCurrent(Long memberId, Long budgetId) {
        Member member = getMemberWithSetup(memberId);
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        return resolveBudget(member, budgetId, today);
    }

    /** {@code budgetId} 미지정 시 현재 주기, 지정 시 본인 소유·이미 시작된 주기만 허용한다(§8.6). */
    private Budget resolveBudget(Member member, Long budgetId, LocalDate today) {
        if (budgetId == null) {
            return budgetCycleService.getOrCreateCurrentBudget(member, today);
        }
        Budget budget = budgetRepository.findByIdAndMember_Id(budgetId, member.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_NOT_FOUND));
        if (budget.getCycleStartDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.BUDGET_FUTURE_CYCLE_NOT_ALLOWED);
        }
        return budget;
    }

    private Member getMemberWithSetup(Long memberId) {
        Member member = getMember(memberId);
        if (!member.hasSpendingGuideSetup()) {
            throw new BusinessException(ErrorCode.SPENDING_GUIDE_SETUP_REQUIRED);
        }
        return member;
    }

    private int clamp(Integer requested, int defaultValue, int max) {
        if (requested == null) {
            return defaultValue;
        }
        return Math.max(1, Math.min(requested, max));
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
