package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.budget.service.SpendingGuideService;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionListResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse.ComparisonInfo;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse.CycleInfo;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse.DailyTrendItem;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse.MerchantTop5Item;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 전체 소비내역 상세 화면(consumeHistoryDetail.md)의 급여주기 요약·결제내역 조회. budgetId 소유권 확인과
 * 현재 주기 조회·생성은 {@link SpendingGuideService#getBudgetOrCurrent}를 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class ConsumptionHistoryService {

    private static final int TRANSACTIONS_PAGE_SIZE_MIN = 1;
    private static final int TRANSACTIONS_PAGE_SIZE_MAX = 30;

    private final SpendingGuideService spendingGuideService;
    private final BudgetRepository budgetRepository;
    private final CardTransactionRepository cardTransactionRepository;

    @Transactional(readOnly = true)
    public ConsumptionHistorySummaryResponse getSummary(Long memberId, Long budgetId) {
        Budget budget = spendingGuideService.getBudgetOrCurrent(memberId, budgetId);
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        boolean completed = budget.getCycleEndDate().isBefore(today);
        LocalDate dataEndDate = completed ? budget.getCycleEndDate() : today;
        String status = completed ? "COMPLETED" : "IN_PROGRESS";

        Optional<Budget> previousBudget = findAdjacentPrevious(memberId, budget);
        Optional<Budget> nextBudget = findAdjacentNext(memberId, budget);

        BigDecimal cycleNetAmount = netSpend(memberId, budget.getCycleStartDate(), dataEndDate);
        BigDecimal canceledAmount = cardTransactionRepository.sumCanceledAmount(
                memberId, TransactionStatus.REJECTED, budget.getCycleStartDate(), dataEndDate);

        ComparisonInfo comparison = previousBudget
                .map(prev -> buildComparison(memberId, budget, prev, dataEndDate, completed, cycleNetAmount))
                .orElse(null);

        List<MerchantTop5Item> merchantTop5 = cardTransactionRepository
                .findMerchantTop5(memberId, TransactionStatus.REJECTED.name(), budget.getCycleStartDate(), dataEndDate)
                .stream()
                .map(row -> new MerchantTop5Item(row.getMerchantKey(), row.getDisplayName(),
                        row.getNetAmount(), row.getTransactionCount()))
                .toList();

        List<DailyTrendItem> dailyTrend = buildDailyTrend(memberId, budget.getCycleStartDate(), dataEndDate);

        CycleInfo cycle = new CycleInfo(budget.getId(), budget.getYearMonth(), budget.getCycleStartDate(),
                budget.getCycleEndDate(), dataEndDate, status,
                previousBudget.map(Budget::getId).orElse(null), nextBudget.map(Budget::getId).orElse(null));

        return new ConsumptionHistorySummaryResponse(cycle, cycleNetAmount, canceledAmount, comparison,
                merchantTop5, dailyTrend);
    }

    @Transactional(readOnly = true)
    public CardTransactionListResponse getTransactions(Long memberId, Long budgetId, int page, int size) {
        if (page < 0 || size < TRANSACTIONS_PAGE_SIZE_MIN || size > TRANSACTIONS_PAGE_SIZE_MAX) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Budget budget = spendingGuideService.getBudgetOrCurrent(memberId, budgetId);
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        LocalDate dataEndDate = budget.getCycleEndDate().isBefore(today) ? budget.getCycleEndDate() : today;

        Pageable pageable = PageRequest.of(page, size);
        Page<CardTransactionResponse> result = cardTransactionRepository.findConsumptionHistoryPage(
                        memberId, TransactionStatus.REJECTED, budget.getCycleStartDate(), dataEndDate, pageable)
                .map(CardTransactionResponse::from);
        return CardTransactionListResponse.from(result);
    }

    /**
     * 직전 연속 주기(§4.1). {@code endDateBefore < 선택 주기 시작일} 중 가장 가까운 후보를 찾은 뒤
     * "끝일+1=선택 주기 시작일"인지 검증한다 — 중간 주기가 비어 있으면 후보를 버려 null(없음)로 취급한다.
     */
    private Optional<Budget> findAdjacentPrevious(Long memberId, Budget budget) {
        return budgetRepository
                .findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(memberId, budget.getCycleStartDate())
                .filter(prev -> prev.getCycleEndDate().plusDays(1).isEqual(budget.getCycleStartDate()));
    }

    /** 다음 연속 주기(§4.1). "시작일=선택 주기 끝일+1"인지 검증한다. */
    private Optional<Budget> findAdjacentNext(Long memberId, Budget budget) {
        return budgetRepository
                .findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(memberId, budget.getCycleEndDate())
                .filter(next -> next.getCycleStartDate().isEqual(budget.getCycleEndDate().plusDays(1)));
    }

    /**
     * 직전 급여주기 대비 증감(§4.3). {@code currentAmount}는 이미 계산된 {@code cycleNetAmount}와 같다
     * (둘 다 cycleStartDate~dataEndDate 범위) — 다시 조회하지 않는다.
     */
    private ComparisonInfo buildComparison(Long memberId, Budget budget, Budget previousBudget,
                                            LocalDate dataEndDate, boolean completed, BigDecimal currentAmount) {
        String basis;
        LocalDate previousDataEndDate;
        if (completed) {
            basis = "FULL_CYCLE";
            previousDataEndDate = previousBudget.getCycleEndDate();
        } else {
            basis = "SAME_ELAPSED_DAYS";
            long selectedDayCount = ChronoUnit.DAYS.between(budget.getCycleStartDate(), dataEndDate) + 1;
            LocalDate candidate = previousBudget.getCycleStartDate().plusDays(selectedDayCount - 1);
            previousDataEndDate = candidate.isAfter(previousBudget.getCycleEndDate())
                    ? previousBudget.getCycleEndDate() : candidate;
        }

        BigDecimal previousAmount = netSpend(memberId, previousBudget.getCycleStartDate(), previousDataEndDate);
        BigDecimal changeAmount = currentAmount.subtract(previousAmount);

        String direction;
        BigDecimal changeRate;
        if (previousAmount.signum() == 0) {
            if (currentAmount.signum() > 0) {
                direction = "NEW";
                changeRate = null;
            } else {
                direction = "UNCHANGED";
                changeRate = new BigDecimal("0.0");
            }
        } else {
            changeRate = changeAmount.abs().multiply(BigDecimal.valueOf(100))
                    .divide(previousAmount.abs(), 1, RoundingMode.HALF_UP);
            if (changeAmount.signum() > 0) {
                direction = "INCREASED";
            } else if (changeAmount.signum() < 0) {
                direction = "DECREASED";
            } else {
                direction = "UNCHANGED";
            }
        }

        return new ComparisonInfo(basis, previousBudget.getId(), currentAmount, previousAmount, changeAmount,
                changeRate, direction);
    }

    /** 일별 소비 추이(§4.5). 거래가 없는 날짜는 0원으로 채운다. */
    private List<DailyTrendItem> buildDailyTrend(Long memberId, LocalDate cycleStartDate, LocalDate dataEndDate) {
        Map<LocalDate, BigDecimal> netByDate = cardTransactionRepository
                .sumNetSpendByDate(memberId, TransactionStatus.REJECTED, cycleStartDate, dataEndDate).stream()
                .collect(Collectors.toMap(CardTransactionRepository.DailyNetAmount::getUsedDate,
                        CardTransactionRepository.DailyNetAmount::getNetAmount));
        return cycleStartDate.datesUntil(dataEndDate.plusDays(1))
                .map(date -> new DailyTrendItem(date, netByDate.getOrDefault(date, BigDecimal.ZERO)))
                .toList();
    }

    /** 공통 거래 조건(§2.2)의 순액 합계. 범위가 비면(끝일이 시작일보다 이르면) 0. */
    private BigDecimal netSpend(Long memberId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }
        return cardTransactionRepository.sumNetSpend(memberId, TransactionStatus.REJECTED, startDate, endDate);
    }
}
