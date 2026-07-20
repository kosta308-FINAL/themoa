package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.cardtransaction.entity.TransactionSource;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.coaching.entity.CoachingCardTargetType;
import com.weaone.themoa.domain.coaching.entity.CoachingDismiss;
import com.weaone.themoa.domain.coaching.entity.CoachingDismissType;
import com.weaone.themoa.domain.coaching.repository.CoachingDismissRepository;
import com.weaone.themoa.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 습관 코칭 규칙 계층(habitExpense.md §3) — 패턴 추출과 절감액 계산. LLM은 이 결과를 문장으로 서술만 한다.
 */
@Service
@RequiredArgsConstructor
public class HabitCoachingCandidateExtractionService {

    private static final BigDecimal MONTHLY_AMOUNT_FLOOR = BigDecimal.valueOf(30_000);
    private static final int MAX_CANDIDATES = 3;
    private static final long STANDARD_MONTH_DAYS = 30;

    private final CardTransactionRepository cardTransactionRepository;
    private final CoachingDismissRepository coachingDismissRepository;

    @Transactional(readOnly = true)
    public List<HabitCoachingCandidate> extractTopCandidates(Member member, LocalDate cycleStart, LocalDate cycleEnd) {
        List<String> categoryCodes = HabitSavingRatioPolicy.CONSUMPTION_CATEGORIES.stream()
                .map(Enum::name)
                .toList();

        List<CardTransactionRepository.HabitCategoryAggregate> categoryAggs = cardTransactionRepository
                .aggregateHabitByCategory(member.getId(), TransactionSource.SYNC, TransactionStatus.REJECTED,
                        cycleStart, cycleEnd, categoryCodes);
        List<CardTransactionRepository.HabitMerchantAliasAggregate> aliasAggs = cardTransactionRepository
                .aggregateHabitByMerchantAlias(member.getId(), TransactionSource.SYNC, TransactionStatus.REJECTED,
                        cycleStart, cycleEnd, categoryCodes);
        Map<Long, List<CardTransactionRepository.HabitMerchantAliasAggregate>> aliasesByCategory = aliasAggs.stream()
                .collect(Collectors.groupingBy(CardTransactionRepository.HabitMerchantAliasAggregate::getCategoryId));

        DismissLookup dismissLookup = DismissLookup.of(coachingDismissRepository.findByMember_Id(member.getId()));
        long cycleDays = ChronoUnit.DAYS.between(cycleStart, cycleEnd) + 1;

        List<HabitCoachingCandidate> pool = new ArrayList<>();
        for (CardTransactionRepository.HabitCategoryAggregate categoryAgg : categoryAggs) {
            List<CardTransactionRepository.HabitMerchantAliasAggregate> aliasesInCategory =
                    aliasesByCategory.getOrDefault(categoryAgg.getCategoryId(), List.of());
            CategoryCode categoryCode = CategoryCode.valueOf(categoryAgg.getCategoryCode());

            // 한 카테고리 안에 이 alias 하나만 있고(=unbranded 잔여도 없이) 그 alias가 카테고리 전체를
            // 이룬다면 더 구체적인 alias 후보로 대체한다. 그 외(여러 alias 분산·unbranded 혼재)에는
            // 카테고리 후보를 쓴다 — 둘을 같이 풀에 넣으면 같은 거래가 두 번 잡혀 상위 3이 중복된다.
            boolean singleDominantAlias = aliasesInCategory.size() == 1
                    && aliasesInCategory.get(0).getTransactionCount() == categoryAgg.getTransactionCount();

            if (singleDominantAlias) {
                CardTransactionRepository.HabitMerchantAliasAggregate alias = aliasesInCategory.get(0);
                if (dismissLookup.isHiddenAlias(alias.getMerchantAliasId())) {
                    continue;
                }
                pool.add(buildCandidate(CoachingCardTargetType.MERCHANT_ALIAS, categoryAgg.getCategoryId(),
                        alias.getMerchantAliasId(), alias.getAliasName(), categoryCode,
                        alias.getTransactionCount(), alias.getNetAmount(), cycleDays,
                        dismissLookup.isToneDownAlias(alias.getMerchantAliasId())));
            } else {
                if (dismissLookup.isHiddenCategory(categoryAgg.getCategoryId())) {
                    continue;
                }
                pool.add(buildCandidate(CoachingCardTargetType.CATEGORY, categoryAgg.getCategoryId(), null,
                        categoryAgg.getCategoryName(), categoryCode,
                        categoryAgg.getTransactionCount(), categoryAgg.getNetAmount(), cycleDays,
                        dismissLookup.isToneDownCategory(categoryAgg.getCategoryId())));
            }
        }

        return pool.stream()
                .filter(candidate -> candidate.monthlyAverage().compareTo(MONTHLY_AMOUNT_FLOOR) >= 0)
                .sorted(Comparator.comparing(HabitCoachingCandidate::monthlyAverage).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    private HabitCoachingCandidate buildCandidate(CoachingCardTargetType targetType, Long categoryId,
                                                    Long merchantAliasId, String label, CategoryCode categoryCode,
                                                    long transactionCount, BigDecimal netAmount, long cycleDays,
                                                    boolean toneDown) {
        BigDecimal avgPerTransaction = netAmount.divide(BigDecimal.valueOf(transactionCount), 0, RoundingMode.HALF_UP);
        BigDecimal monthlyAverage = netAmount.multiply(BigDecimal.valueOf(STANDARD_MONTH_DAYS))
                .divide(BigDecimal.valueOf(cycleDays), 0, RoundingMode.HALF_UP);
        BigDecimal estimatedSaving = monthlyAverage.multiply(HabitSavingRatioPolicy.ratioFor(categoryCode))
                .setScale(0, RoundingMode.HALF_UP);
        return new HabitCoachingCandidate(targetType, categoryId, merchantAliasId, label, transactionCount,
                netAmount, avgPerTransaction, monthlyAverage, estimatedSaving, toneDown);
    }

    /** dismiss 반영(§5): HIDE는 후보에서 제외, NOT_WASTE는 톤다운 힌트로만 남긴다. */
    private record DismissLookup(Set<Long> hideCategoryIds, Set<Long> hideAliasIds,
                                  Set<Long> toneDownCategoryIds, Set<Long> toneDownAliasIds) {

        static DismissLookup of(List<CoachingDismiss> dismisses) {
            Set<Long> hideCategoryIds = idsOf(dismisses, CoachingDismissType.HIDE, true);
            Set<Long> hideAliasIds = idsOf(dismisses, CoachingDismissType.HIDE, false);
            Set<Long> toneDownCategoryIds = idsOf(dismisses, CoachingDismissType.NOT_WASTE, true);
            Set<Long> toneDownAliasIds = idsOf(dismisses, CoachingDismissType.NOT_WASTE, false);
            return new DismissLookup(hideCategoryIds, hideAliasIds, toneDownCategoryIds, toneDownAliasIds);
        }

        private static Set<Long> idsOf(List<CoachingDismiss> dismisses, CoachingDismissType type, boolean category) {
            return dismisses.stream()
                    .filter(d -> d.getDismissType() == type)
                    .filter(d -> category ? d.getCategory() != null : d.getMerchantAlias() != null)
                    .map(d -> category ? d.getCategory().getId() : d.getMerchantAlias().getId())
                    .collect(Collectors.toSet());
        }

        boolean isHiddenCategory(Long categoryId) {
            return hideCategoryIds.contains(categoryId);
        }

        boolean isHiddenAlias(Long aliasId) {
            return hideAliasIds.contains(aliasId);
        }

        boolean isToneDownCategory(Long categoryId) {
            return toneDownCategoryIds.contains(categoryId);
        }

        boolean isToneDownAlias(Long aliasId) {
            return toneDownAliasIds.contains(aliasId);
        }
    }
}
