package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.budget.service.SpendingGuideService;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/** 전체 소비내역 상세 급여주기 요약(consumeHistoryDetail.md §4·§7.3·§10.2) 검증. */
@ExtendWith(MockitoExtension.class)
class ConsumptionHistoryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);

    @Mock
    private SpendingGuideService spendingGuideService;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private CardTransactionRepository cardTransactionRepository;

    private ConsumptionHistoryService service() {
        return new ConsumptionHistoryService(spendingGuideService, budgetRepository, cardTransactionRepository);
    }

    private Member member() {
        return Member.signUp("u@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now());
    }

    private Budget budget(LocalDate start, LocalDate end) {
        return budget(null, start, end);
    }

    private Budget budget(Long id, LocalDate start, LocalDate end) {
        Budget budget = Budget.openCycle(member(), start.toString().substring(0, 7), start, end,
                BigDecimal.valueOf(2_000_000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        if (id != null) {
            ReflectionTestUtils.setField(budget, "id", id);
        }
        return budget;
    }

    private void stubEmptyAggregates() {
        given(cardTransactionRepository.sumCanceledAmount(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), any(), any()))
                .willReturn(BigDecimal.ZERO);
        given(cardTransactionRepository.findMerchantTop5(eq(MEMBER_ID), eq(TransactionStatus.REJECTED.name()), any(), any()))
                .willReturn(List.of());
        given(cardTransactionRepository.sumNetSpendByDate(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), any(), any()))
                .willReturn(List.of());
    }

    @Test
    @DisplayName("진행 중인 주기는 오늘까지만 집계하고 dataEndDate=오늘, status=IN_PROGRESS다")
    void inProgressCycleAggregatesThroughToday() {
        Budget budget = budget(TODAY.minusDays(5), TODAY.plusDays(20));
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, null)).willReturn(budget);
        given(budgetRepository.findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(budgetRepository.findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(cardTransactionRepository.sumNetSpend(MEMBER_ID, TransactionStatus.REJECTED,
                budget.getCycleStartDate(), TODAY)).willReturn(new BigDecimal("120000"));
        stubEmptyAggregates();

        ConsumptionHistorySummaryResponse response = service().getSummary(MEMBER_ID, null);

        assertThat(response.cycle().status()).isEqualTo("IN_PROGRESS");
        assertThat(response.cycle().dataEndDate()).isEqualTo(TODAY);
        assertThat(response.cycleNetAmount()).isEqualByComparingTo("120000");
    }

    @Test
    @DisplayName("완료된 주기는 dataEndDate=cycleEndDate, status=COMPLETED다")
    void completedCycleUsesFullRange() {
        LocalDate start = TODAY.minusMonths(2);
        LocalDate end = TODAY.minusMonths(1).minusDays(1);
        Budget budget = budget(start, end);
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, 5L)).willReturn(budget);
        given(budgetRepository.findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(budgetRepository.findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(cardTransactionRepository.sumNetSpend(MEMBER_ID, TransactionStatus.REJECTED, start, end))
                .willReturn(new BigDecimal("300000"));
        stubEmptyAggregates();

        ConsumptionHistorySummaryResponse response = service().getSummary(MEMBER_ID, 5L);

        assertThat(response.cycle().status()).isEqualTo("COMPLETED");
        assertThat(response.cycle().dataEndDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("직전 budget이 연속되지 않으면(중간 주기 누락) comparison이 null이다")
    void nonAdjacentPreviousBudgetIsIgnored() {
        LocalDate start = TODAY.minusDays(5);
        LocalDate end = TODAY.plusDays(20);
        Budget budget = budget(start, end);
        Budget nonAdjacentPrevious = budget(start.minusMonths(2), start.minusDays(10)); // 끝일+1 != start
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, null)).willReturn(budget);
        given(budgetRepository.findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(MEMBER_ID, start))
                .willReturn(Optional.of(nonAdjacentPrevious));
        given(budgetRepository.findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(cardTransactionRepository.sumNetSpend(MEMBER_ID, TransactionStatus.REJECTED, start, TODAY))
                .willReturn(BigDecimal.ZERO);
        stubEmptyAggregates();

        ConsumptionHistorySummaryResponse response = service().getSummary(MEMBER_ID, null);

        assertThat(response.comparison()).isNull();
        assertThat(response.cycle().previousBudgetId()).isNull();
    }

    @Test
    @DisplayName("직전 주기가 연속이면 진행 중 주기는 같은 경과일만 비교하고 증감률을 소수 첫째 자리 반올림한다")
    void adjacentPreviousBudgetComparesSameElapsedDays() {
        LocalDate start = TODAY.minusDays(9); // 경과일 = 10일(오늘 포함)
        LocalDate end = TODAY.plusDays(20);
        Budget budget = budget(start, end);
        LocalDate previousStart = start.minusMonths(1);
        LocalDate previousEnd = start.minusDays(1);
        Budget previous = budget(30L, previousStart, previousEnd);
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, null)).willReturn(budget);
        given(budgetRepository.findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(MEMBER_ID, start))
                .willReturn(Optional.of(previous));
        given(budgetRepository.findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(cardTransactionRepository.sumNetSpend(MEMBER_ID, TransactionStatus.REJECTED, start, TODAY))
                .willReturn(new BigDecimal("90000"));
        LocalDate previousDataEndDate = previousStart.plusDays(9); // selectedDayCount(10)-1
        given(cardTransactionRepository.sumNetSpend(MEMBER_ID, TransactionStatus.REJECTED, previousStart, previousDataEndDate))
                .willReturn(new BigDecimal("100000"));
        stubEmptyAggregates();

        ConsumptionHistorySummaryResponse response = service().getSummary(MEMBER_ID, null);

        assertThat(response.comparison()).isNotNull();
        assertThat(response.comparison().basis()).isEqualTo("SAME_ELAPSED_DAYS");
        assertThat(response.comparison().previousBudgetId()).isEqualTo(30L);
        assertThat(response.comparison().changeAmount()).isEqualByComparingTo("-10000");
        assertThat(response.comparison().changeRate()).isEqualByComparingTo("10.0");
        assertThat(response.comparison().direction()).isEqualTo("DECREASED");
    }

    @Test
    @DisplayName("직전 주기 순사용액이 0원이고 이번 주기 사용액이 있으면 direction=NEW, changeRate=null이다")
    void zeroPreviousAmountWithSpendIsNew() {
        LocalDate start = TODAY.minusDays(2);
        LocalDate end = TODAY.plusDays(20);
        Budget budget = budget(start, end);
        LocalDate previousStart = start.minusMonths(1);
        LocalDate previousEnd = start.minusDays(1);
        Budget previous = budget(previousStart, previousEnd);
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, null)).willReturn(budget);
        given(budgetRepository.findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(MEMBER_ID, start))
                .willReturn(Optional.of(previous));
        given(budgetRepository.findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(cardTransactionRepository.sumNetSpend(MEMBER_ID, TransactionStatus.REJECTED, start, TODAY))
                .willReturn(new BigDecimal("5000"));
        given(cardTransactionRepository.sumNetSpend(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), eq(previousStart), any()))
                .willReturn(BigDecimal.ZERO);
        stubEmptyAggregates();

        ConsumptionHistorySummaryResponse response = service().getSummary(MEMBER_ID, null);

        assertThat(response.comparison().direction()).isEqualTo("NEW");
        assertThat(response.comparison().changeRate()).isNull();
    }

    @Test
    @DisplayName("30~31일 완료 주기의 일별 배열은 모든 날짜를 포함하고 거래 없는 날짜는 0원이다")
    void dailyTrendFillsEveryDateWithZero() {
        LocalDate start = LocalDate.of(2026, 6, 5);
        LocalDate end = LocalDate.of(2026, 7, 4); // 30일 완료 주기(오늘 이후로 가정하기 위해 과거로 고정)
        Budget budget = budget(start, end);
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, 9L)).willReturn(budget);
        given(budgetRepository.findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(budgetRepository.findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(eq(MEMBER_ID), any()))
                .willReturn(Optional.empty());
        given(cardTransactionRepository.sumNetSpend(MEMBER_ID, TransactionStatus.REJECTED, start, end))
                .willReturn(new BigDecimal("10000"));
        given(cardTransactionRepository.sumCanceledAmount(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), any(), any()))
                .willReturn(BigDecimal.ZERO);
        given(cardTransactionRepository.findMerchantTop5(eq(MEMBER_ID), eq(TransactionStatus.REJECTED.name()), any(), any()))
                .willReturn(List.of());
        given(cardTransactionRepository.sumNetSpendByDate(MEMBER_ID, TransactionStatus.REJECTED, start, end))
                .willReturn(List.of(dailyRow(start.plusDays(2), new BigDecimal("10000"))));

        ConsumptionHistorySummaryResponse response = service().getSummary(MEMBER_ID, 9L);

        assertThat(response.dailyTrend()).hasSize(30);
        assertThat(response.dailyTrend().get(0).date()).isEqualTo(start);
        assertThat(response.dailyTrend().get(29).date()).isEqualTo(end);
        assertThat(response.dailyTrend().get(2).netAmount()).isEqualByComparingTo("10000");
        assertThat(response.dailyTrend().get(0).netAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("결제내역 페이지는 page<0이면 400, size가 1~30 밖이면 400이다")
    void transactionsRejectsInvalidPageOrSize() {
        assertThatThrownBy(() -> service().getTransactions(MEMBER_ID, null, -1, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service().getTransactions(MEMBER_ID, null, 0, 31))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service().getTransactions(MEMBER_ID, null, 0, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("budgetId 소유권·미래 주기 오류는 SpendingGuideService의 예외를 그대로 전파한다")
    void propagatesBudgetResolutionErrors() {
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, 99L))
                .willThrow(new BusinessException(ErrorCode.BUDGET_NOT_FOUND));

        assertThatThrownBy(() -> service().getSummary(MEMBER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUDGET_NOT_FOUND);
    }

    private CardTransactionRepository.DailyNetAmount dailyRow(LocalDate date, BigDecimal amount) {
        return new CardTransactionRepository.DailyNetAmount() {
            @Override
            public LocalDate getUsedDate() {
                return date;
            }

            @Override
            public BigDecimal getNetAmount() {
                return amount;
            }
        };
    }
}
