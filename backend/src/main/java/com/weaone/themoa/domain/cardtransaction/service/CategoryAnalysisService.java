package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.cardtransaction.dto.response.AmountPercentageResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategoryAnalysisCycleResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategoryAnalysisResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategoryComparisonResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategoryDayTypeResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategoryInsightResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategoryPhaseResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategoryTrendPointResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.SelectedCategoryAnalysisResponse;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 카테고리 소비 상세 화면(categoryDetail.md) 유스케이스. {@link com.weaone.themoa.domain.cardtransaction.service.CardTransactionQueryService}를
 * 비대하게 만들지 않도록 별도 서비스로 분리했다(§9).
 */
@Service
@RequiredArgsConstructor
public class CategoryAnalysisService {

    private static final int EARLY_PHASE_LAST_DAY = 10;
    private static final int MIDDLE_PHASE_LAST_DAY = 20;
    private static final int MIN_TREND_CYCLE_COUNT_FOR_TREND_INSIGHT = 3;
    private static final int MIN_TREND_CYCLE_COUNT_FOR_ENOUGH_HISTORY = 3;

    private final MemberRepository memberRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetCycleService budgetCycleService;
    private final CategoryRepository categoryRepository;
    private final CardTransactionRepository cardTransactionRepository;

    @Transactional
    public CategoryAnalysisResponse analyze(Long memberId, Long budgetId, Long categoryId) {
        validatePositive(budgetId);
        validatePositive(categoryId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        Budget selected = resolveSelectedBudget(member, budgetId, today);

        LocalDate selectedDataEndDate = selected.getCycleEndDate().isBefore(today) ? selected.getCycleEndDate() : today;
        CycleStatus status = selected.getCycleEndDate().isBefore(today) ? CycleStatus.COMPLETED : CycleStatus.IN_PROGRESS;
        int selectedComparedDayCount = (int) ChronoUnit.DAYS.between(selected.getCycleStartDate(), selectedDataEndDate) + 1;

        Budget previousBudget = resolvePreviousBudget(memberId, selected);
        Budget nextBudget = resolveNextBudget(memberId, selected);
        List<Budget> trendBudgets = resolveTrendBudgets(memberId, selected);

        Map<Long, List<CardTransactionRepository.CategorySummary>> summariesByBudgetId = new LinkedHashMap<>();
        Map<Long, LocalDate> dataEndDateByBudgetId = new HashMap<>();
        Map<Long, Integer> comparedDayCountByBudgetId = new HashMap<>();
        for (Budget cycleBudget : trendBudgets) {
            LocalDate dataEndDate;
            int comparedDayCount;
            if (cycleBudget.getId().equals(selected.getId())) {
                dataEndDate = selectedDataEndDate;
                comparedDayCount = selectedComparedDayCount;
            } else {
                dataEndDate = clipDataEndDate(cycleBudget, selectedComparedDayCount);
                comparedDayCount = (int) ChronoUnit.DAYS.between(cycleBudget.getCycleStartDate(), dataEndDate) + 1;
            }
            dataEndDateByBudgetId.put(cycleBudget.getId(), dataEndDate);
            comparedDayCountByBudgetId.put(cycleBudget.getId(), comparedDayCount);
            summariesByBudgetId.put(cycleBudget.getId(), cardTransactionRepository.summarizeByCategory(
                    memberId, TransactionStatus.REJECTED, cycleBudget.getCycleStartDate(), dataEndDate));
        }

        List<CardTransactionRepository.CategorySummary> selectedSummaries = summariesByBudgetId.get(selected.getId());
        List<CardTransactionRepository.CategorySummary> previousSummaries = previousBudget != null
                ? summariesByBudgetId.getOrDefault(previousBudget.getId(), List.of())
                : List.of();

        Map<Long, CardTransactionRepository.CategorySummary> selectedMap = indexByCategoryId(selectedSummaries);
        Map<Long, CardTransactionRepository.CategorySummary> previousMap = indexByCategoryId(previousSummaries);

        BigDecimal selectedCyclePositiveTotal = sumAmounts(selectedSummaries);
        BigDecimal previousCyclePositiveTotal = sumAmounts(previousSummaries);

        List<CategoryComparisonResponse> categories = buildComparisonList(
                selectedMap, previousMap, selectedCyclePositiveTotal, previousBudget != null);

        Long resolvedCategoryId;
        String resolvedCategoryName;
        boolean noCategorySpend = false;
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            resolvedCategoryId = categoryId;
            resolvedCategoryName = category.getName();
        } else {
            CardTransactionRepository.CategorySummary defaultPick = pickDefaultCategory(selectedSummaries, previousSummaries);
            if (defaultPick == null) {
                resolvedCategoryId = null;
                resolvedCategoryName = null;
                noCategorySpend = true;
            } else {
                resolvedCategoryId = defaultPick.getCategoryId();
                resolvedCategoryName = defaultPick.getCategoryName();
            }
        }

        SelectedCategoryAnalysisResponse selectedCategoryResponse = null;
        if (resolvedCategoryId != null) {
            selectedCategoryResponse = buildSelectedCategoryAnalysis(memberId, resolvedCategoryId, resolvedCategoryName,
                    selected, previousBudget, trendBudgets, summariesByBudgetId, dataEndDateByBudgetId,
                    comparedDayCountByBudgetId, selectedDataEndDate);
        }

        CategoryAnalysisCycleResponse cycleResponse = new CategoryAnalysisCycleResponse(
                selected.getId(), selected.getYearMonth(), selected.getCycleStartDate(), selected.getCycleEndDate(),
                selectedDataEndDate, status, selectedComparedDayCount,
                previousBudget != null ? previousBudget.getId() : null,
                nextBudget != null ? nextBudget.getId() : null);

        return new CategoryAnalysisResponse(cycleResponse, CategoryAnalysisResponse.COMPARISON_BASIS_SAME_ELAPSED_DAYS,
                selectedCyclePositiveTotal, previousCyclePositiveTotal, categories, selectedCategoryResponse,
                noCategorySpend ? CategoryAnalysisResponse.EMPTY_REASON_NO_CATEGORY_SPEND : null);
    }

    private void validatePositive(Long value) {
        if (value != null && value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /** §5.1: budgetId 지정 시 본인 소유 예산만 허용, 생략 시 현재 주기를 조회·생성한다. */
    private Budget resolveSelectedBudget(Member member, Long budgetId, LocalDate today) {
        if (budgetId != null) {
            Budget budget = budgetRepository.findByIdAndMember_Id(budgetId, member.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_NOT_FOUND));
            if (budget.getCycleStartDate().isAfter(today)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            return budget;
        }
        if (!member.hasSpendingGuideSetup()) {
            throw new BusinessException(ErrorCode.SPENDING_GUIDE_SETUP_REQUIRED);
        }
        return budgetCycleService.getOrCreateCurrentBudget(member, today);
    }

    /** §5.3: 종료일+1=선택 주기 시작일인 경우에만 실제 직전 주기로 인정한다. */
    private Budget resolvePreviousBudget(Long memberId, Budget selected) {
        return budgetRepository
                .findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(memberId, selected.getCycleStartDate())
                .filter(candidate -> candidate.getCycleEndDate().plusDays(1).isEqual(selected.getCycleStartDate()))
                .orElse(null);
    }

    /** §5.3: 선택 주기 종료일+1=다음 주기 시작일인 경우에만 다음 주기로 인정한다. 미래 주기는 반환하지 않는다. */
    private Budget resolveNextBudget(Long memberId, Budget selected) {
        return budgetRepository
                .findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(memberId, selected.getCycleEndDate())
                .filter(candidate -> candidate.getCycleStartDate().isEqual(selected.getCycleEndDate().plusDays(1)))
                .orElse(null);
    }

    /** §6.5: 선택 주기를 마지막 점으로 최근 연속된 최대 4개 주기를 오래된 순으로 반환한다. */
    private List<Budget> resolveTrendBudgets(Long memberId, Budget selected) {
        List<Budget> candidatesDesc = budgetRepository
                .findTop4ByMember_IdAndCycleStartDateLessThanEqualOrderByCycleStartDateDesc(
                        memberId, selected.getCycleStartDate());

        List<Budget> continuousDesc = new ArrayList<>();
        continuousDesc.add(selected);
        Budget cursor = selected;
        for (Budget candidate : candidatesDesc) {
            if (candidate.getId().equals(selected.getId())) {
                continue;
            }
            if (candidate.getCycleEndDate().plusDays(1).isEqual(cursor.getCycleStartDate())) {
                continuousDesc.add(candidate);
                cursor = candidate;
            } else {
                break;
            }
        }
        List<Budget> oldestFirst = new ArrayList<>(continuousDesc);
        Collections.reverse(oldestFirst);
        return oldestFirst;
    }

    /** §6.2: targetDataEndDate = min(targetCycleStartDate + (comparedDayCount − 1일), targetCycleEndDate). */
    private LocalDate clipDataEndDate(Budget target, int selectedComparedDayCount) {
        LocalDate candidate = target.getCycleStartDate().plusDays(selectedComparedDayCount - 1L);
        return candidate.isAfter(target.getCycleEndDate()) ? target.getCycleEndDate() : candidate;
    }

    private Map<Long, CardTransactionRepository.CategorySummary> indexByCategoryId(
            List<CardTransactionRepository.CategorySummary> summaries) {
        Map<Long, CardTransactionRepository.CategorySummary> map = new HashMap<>();
        for (CardTransactionRepository.CategorySummary summary : summaries) {
            map.put(summary.getCategoryId(), summary);
        }
        return map;
    }

    private BigDecimal sumAmounts(List<CardTransactionRepository.CategorySummary> summaries) {
        BigDecimal total = BigDecimal.ZERO;
        for (CardTransactionRepository.CategorySummary summary : summaries) {
            total = total.add(summary.getTotalAmount());
        }
        return total;
    }

    /** §6.3·§6.4: 선택·직전 주기 카테고리 목록을 합쳐 비중·증감을 계산한다. */
    private List<CategoryComparisonResponse> buildComparisonList(
            Map<Long, CardTransactionRepository.CategorySummary> selectedMap,
            Map<Long, CardTransactionRepository.CategorySummary> previousMap,
            BigDecimal selectedCyclePositiveTotal, boolean previousCycleExists) {

        Map<Long, String> categoryNames = new LinkedHashMap<>();
        selectedMap.forEach((id, summary) -> categoryNames.put(id, summary.getCategoryName()));
        previousMap.forEach((id, summary) -> categoryNames.putIfAbsent(id, summary.getCategoryName()));

        List<CategoryComparisonResponse> categories = new ArrayList<>();
        for (Map.Entry<Long, String> entry : categoryNames.entrySet()) {
            Long categoryId = entry.getKey();
            BigDecimal selectedAmount = amountOf(selectedMap.get(categoryId));
            BigDecimal previousAmount = amountOf(previousMap.get(categoryId));
            ChangeResult change = computeChange(selectedAmount, previousAmount, previousCycleExists);
            BigDecimal selectedShare = percentageOf(selectedAmount, selectedCyclePositiveTotal);
            categories.add(new CategoryComparisonResponse(categoryId, entry.getValue(), selectedAmount, previousAmount,
                    selectedShare, change.changeAmount(), change.rate(), change.direction()));
        }
        categories.sort(Comparator
                .comparing(CategoryComparisonResponse::selectedAmount, Comparator.reverseOrder())
                .thenComparing(CategoryComparisonResponse::previousAmount, Comparator.reverseOrder())
                .thenComparing(CategoryComparisonResponse::categoryId));
        return categories;
    }

    private BigDecimal amountOf(CardTransactionRepository.CategorySummary summary) {
        return summary != null ? summary.getTotalAmount() : BigDecimal.ZERO;
    }

    /** §5.4: 선택 주기 최대 소비 카테고리, 없으면 직전 주기 최대 소비 카테고리, 동률이면 categoryId가 작은 쪽. */
    private CardTransactionRepository.CategorySummary pickDefaultCategory(
            List<CardTransactionRepository.CategorySummary> selectedSummaries,
            List<CardTransactionRepository.CategorySummary> previousSummaries) {
        CardTransactionRepository.CategorySummary bestSelected = pickMaxAmount(selectedSummaries);
        return bestSelected != null ? bestSelected : pickMaxAmount(previousSummaries);
    }

    private CardTransactionRepository.CategorySummary pickMaxAmount(
            List<CardTransactionRepository.CategorySummary> summaries) {
        CardTransactionRepository.CategorySummary best = null;
        for (CardTransactionRepository.CategorySummary candidate : summaries) {
            if (best == null) {
                best = candidate;
                continue;
            }
            int compared = candidate.getTotalAmount().compareTo(best.getTotalAmount());
            if (compared > 0 || (compared == 0 && candidate.getCategoryId() < best.getCategoryId())) {
                best = candidate;
            }
        }
        return best;
    }

    private SelectedCategoryAnalysisResponse buildSelectedCategoryAnalysis(
            Long memberId, Long categoryId, String categoryName, Budget selected, Budget previousBudget,
            List<Budget> trendBudgets, Map<Long, List<CardTransactionRepository.CategorySummary>> summariesByBudgetId,
            Map<Long, LocalDate> dataEndDateByBudgetId, Map<Long, Integer> comparedDayCountByBudgetId,
            LocalDate selectedDataEndDate) {

        BigDecimal selectedAmount = amountFor(summariesByBudgetId.get(selected.getId()), categoryId);
        BigDecimal previousAmount = previousBudget != null
                ? amountFor(summariesByBudgetId.getOrDefault(previousBudget.getId(), List.of()), categoryId)
                : BigDecimal.ZERO;
        boolean previousCycleExists = previousBudget != null;
        ChangeResult change = computeChange(selectedAmount, previousAmount, previousCycleExists);

        CategoryHistoryStatus historyStatus = trendBudgets.size() >= MIN_TREND_CYCLE_COUNT_FOR_ENOUGH_HISTORY
                ? CategoryHistoryStatus.ENOUGH : CategoryHistoryStatus.INSUFFICIENT;

        List<CategoryTrendPointResponse> trend = trendBudgets.stream()
                .map(budget -> new CategoryTrendPointResponse(
                        budget.getId(), budget.getYearMonth(), budget.getCycleStartDate(), budget.getCycleEndDate(),
                        dataEndDateByBudgetId.get(budget.getId()), comparedDayCountByBudgetId.get(budget.getId()),
                        amountFor(summariesByBudgetId.get(budget.getId()), categoryId)))
                .toList();

        List<CardTransactionRepository.DailyCategoryAmount> dailyAmounts = cardTransactionRepository
                .summarizeDailyByCategory(memberId, categoryId, TransactionStatus.REJECTED,
                        selected.getCycleStartDate(), selectedDataEndDate);

        CategoryPhaseResponse phase = buildPhase(dailyAmounts, selected.getCycleStartDate(), selectedAmount);
        CategoryDayTypeResponse dayType = buildDayType(dailyAmounts, selectedAmount);

        List<CategoryInsightResponse> insights = buildInsights(change, previousCycleExists, phase, selectedAmount, trend);

        return new SelectedCategoryAnalysisResponse(categoryId, categoryName, selectedAmount, previousAmount,
                historyStatus, trend, phase, dayType, insights);
    }

    private BigDecimal amountFor(List<CardTransactionRepository.CategorySummary> summaries, Long categoryId) {
        if (summaries == null) {
            return BigDecimal.ZERO;
        }
        for (CardTransactionRepository.CategorySummary summary : summaries) {
            if (summary.getCategoryId().equals(categoryId)) {
                return summary.getTotalAmount();
            }
        }
        return BigDecimal.ZERO;
    }

    /** §6.6: 급여주기 시작일로부터의 경과일로 초·중·후반을 나눈다. */
    private CategoryPhaseResponse buildPhase(List<CardTransactionRepository.DailyCategoryAmount> dailyAmounts,
                                              LocalDate cycleStartDate, BigDecimal selectedAmount) {
        BigDecimal earlyAmount = BigDecimal.ZERO;
        BigDecimal middleAmount = BigDecimal.ZERO;
        BigDecimal lateAmount = BigDecimal.ZERO;
        for (CardTransactionRepository.DailyCategoryAmount daily : dailyAmounts) {
            long dayNumber = ChronoUnit.DAYS.between(cycleStartDate, daily.getUsedDate()) + 1;
            if (dayNumber <= EARLY_PHASE_LAST_DAY) {
                earlyAmount = earlyAmount.add(daily.getTotalAmount());
            } else if (dayNumber <= MIDDLE_PHASE_LAST_DAY) {
                middleAmount = middleAmount.add(daily.getTotalAmount());
            } else {
                lateAmount = lateAmount.add(daily.getTotalAmount());
            }
        }
        return new CategoryPhaseResponse(
                new AmountPercentageResponse(earlyAmount, percentageOf(earlyAmount, selectedAmount)),
                new AmountPercentageResponse(middleAmount, percentageOf(middleAmount, selectedAmount)),
                new AmountPercentageResponse(lateAmount, percentageOf(lateAmount, selectedAmount)));
    }

    /** §6.7: 공휴일은 별도 처리하지 않고 요일만으로 평일·주말을 나눈다. */
    private CategoryDayTypeResponse buildDayType(List<CardTransactionRepository.DailyCategoryAmount> dailyAmounts,
                                                  BigDecimal selectedAmount) {
        BigDecimal weekdayAmount = BigDecimal.ZERO;
        BigDecimal weekendAmount = BigDecimal.ZERO;
        for (CardTransactionRepository.DailyCategoryAmount daily : dailyAmounts) {
            DayOfWeek dayOfWeek = daily.getUsedDate().getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                weekendAmount = weekendAmount.add(daily.getTotalAmount());
            } else {
                weekdayAmount = weekdayAmount.add(daily.getTotalAmount());
            }
        }
        return new CategoryDayTypeResponse(
                new AmountPercentageResponse(weekdayAmount, percentageOf(weekdayAmount, selectedAmount)),
                new AmountPercentageResponse(weekendAmount, percentageOf(weekendAmount, selectedAmount)));
    }

    /** §7: PERIOD_CHANGE → PEAK_PHASE → CYCLE_TREND 순서로, 조건을 만족하는 항목만 반환한다. */
    private List<CategoryInsightResponse> buildInsights(ChangeResult change, boolean previousCycleExists,
                                                          CategoryPhaseResponse phase, BigDecimal selectedAmount,
                                                          List<CategoryTrendPointResponse> trend) {
        List<CategoryInsightResponse> insights = new ArrayList<>();
        if (previousCycleExists) {
            insights.add(new CategoryInsightResponse(CategoryInsightType.PERIOD_CHANGE, change.direction(),
                    change.changeAmount(), change.rate(), null, null, null));
        }
        if (selectedAmount.signum() > 0) {
            CyclePhase peakPhase = maxPhase(phase);
            AmountPercentageResponse peakValue = phaseValue(phase, peakPhase);
            insights.add(new CategoryInsightResponse(CategoryInsightType.PEAK_PHASE, null,
                    peakValue.amount(), null, peakPhase, peakValue.percentage(), null));
        }
        if (trend.size() >= MIN_TREND_CYCLE_COUNT_FOR_TREND_INSIGHT) {
            ChangeDirection direction = trendDirection(trend);
            insights.add(new CategoryInsightResponse(CategoryInsightType.CYCLE_TREND, direction,
                    null, null, null, null, trend.size()));
        }
        return insights;
    }

    /** 동률이면 EARLY → MIDDLE → LATE 순으로 선택한다(§7.2). */
    private CyclePhase maxPhase(CategoryPhaseResponse phase) {
        BigDecimal max = phase.early().amount();
        CyclePhase peak = CyclePhase.EARLY;
        if (phase.middle().amount().compareTo(max) > 0) {
            max = phase.middle().amount();
            peak = CyclePhase.MIDDLE;
        }
        if (phase.late().amount().compareTo(max) > 0) {
            peak = CyclePhase.LATE;
        }
        return peak;
    }

    private AmountPercentageResponse phaseValue(CategoryPhaseResponse phase, CyclePhase target) {
        return switch (target) {
            case EARLY -> phase.early();
            case MIDDLE -> phase.middle();
            case LATE -> phase.late();
        };
    }

    /** §7.3: 오래된 순 추이를 순서대로 비교해 엄격 증가/감소/동일/혼합을 판정한다. */
    private ChangeDirection trendDirection(List<CategoryTrendPointResponse> trend) {
        boolean increasing = true;
        boolean decreasing = true;
        boolean allEqual = true;
        for (int i = 1; i < trend.size(); i++) {
            int compared = trend.get(i).amount().compareTo(trend.get(i - 1).amount());
            if (compared <= 0) {
                increasing = false;
            }
            if (compared >= 0) {
                decreasing = false;
            }
            if (compared != 0) {
                allEqual = false;
            }
        }
        if (allEqual) {
            return ChangeDirection.UNCHANGED;
        }
        if (increasing) {
            return ChangeDirection.INCREASED;
        }
        if (decreasing) {
            return ChangeDirection.DECREASED;
        }
        return ChangeDirection.MIXED;
    }

    /** §6.4: 증감액·증감률·증감 상태. */
    private ChangeResult computeChange(BigDecimal selectedAmount, BigDecimal previousAmount, boolean previousCycleExists) {
        BigDecimal changeAmount = selectedAmount.subtract(previousAmount);
        if (!previousCycleExists) {
            return new ChangeResult(changeAmount, ChangeDirection.UNCHANGED, null);
        }
        if (previousAmount.signum() == 0) {
            ChangeDirection direction = selectedAmount.signum() > 0 ? ChangeDirection.NEW : ChangeDirection.UNCHANGED;
            return new ChangeResult(changeAmount, direction, null);
        }
        int compared = selectedAmount.compareTo(previousAmount);
        if (compared == 0) {
            return new ChangeResult(changeAmount, ChangeDirection.UNCHANGED, BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        }
        BigDecimal rate = changeAmount.multiply(BigDecimal.valueOf(100)).divide(previousAmount, 1, RoundingMode.HALF_UP);
        return new ChangeResult(changeAmount, compared > 0 ? ChangeDirection.INCREASED : ChangeDirection.DECREASED, rate);
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    private record ChangeResult(BigDecimal changeAmount, ChangeDirection direction, BigDecimal rate) {
    }
}
