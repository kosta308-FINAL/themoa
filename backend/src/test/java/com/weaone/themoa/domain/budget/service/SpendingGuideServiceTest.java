package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.dto.response.SpendingGuideSummaryResponse;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.cardtransaction.dto.response.CategorySummaryListResponse;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.cardtransaction.support.BackfillWindowPolicy;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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

/** 오늘 현황 요약 계산(dailyBudget.md §1·§3, dayguide.md §3.1) 검증. */
@ExtendWith(MockitoExtension.class)
class SpendingGuideServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final ZoneOffsetToday TODAY = new ZoneOffsetToday();

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetCycleService budgetCycleService;
    @Mock
    private CardTransactionRepository cardTransactionRepository;

    private SpendingGuideService service() {
        return new SpendingGuideService(memberRepository, budgetRepository, budgetCycleService, cardTransactionRepository);
    }

    private Member member(BigDecimal salary, Integer payday, BigDecimal savingsTarget) {
        Member member = Member.signUp("u@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now());
        if (salary != null && payday != null) {
            member.configureSpendingGuide(salary, payday);
        }
        if (savingsTarget != null) {
            member.changeSavingsTarget(savingsTarget);
        }
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private Budget budget(Member member, LocalDate start, LocalDate end, String salary, String savings, String fixed) {
        return Budget.openCycle(member, "2026-07", start, end,
                new BigDecimal(salary), new BigDecimal(savings), new BigDecimal(fixed), BigDecimal.ZERO);
    }

    private void stubNetSpend(LocalDate start, LocalDate end, String amount) {
        given(cardTransactionRepository.sumNetSpend(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), eq(start), eq(end)))
                .willReturn(new BigDecimal(amount));
    }

    @Test
    @DisplayName("월급·급여일 미등록이면 오류가 아니라 setupRequired=true + missingFields로 반환한다")
    void setupRequiredWhenMissing() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member(null, null, null)));

        SpendingGuideSummaryResponse response = service().getSummary(MEMBER_ID);

        assertThat(response.setupRequired()).isTrue();
        assertThat(response.missingFields()).containsExactlyInAnyOrder("salaryAmount", "payday");
    }

    @Test
    @DisplayName("하루 권장액은 어제까지 순지출만 쓰고, 오늘 순사용액은 오늘 사용 가능 금액·남은 예산에만 반영된다")
    void dailyRecommendedIgnoresTodaySpend() {
        LocalDate today = TODAY.value();
        LocalDate start = today.minusDays(5);
        LocalDate end = today.plusDays(19); // 남은일수 20
        Member member = member(new BigDecimal("1000000"), 5, BigDecimal.ZERO);
        Budget budget = budget(member, start, end, "1000000", "0", "0");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(budgetCycleService.getOrCreateCurrentBudget(any(), any())).willReturn(budget);
        stubNetSpend(start, today.minusDays(1), "250000"); // 어제까지
        stubNetSpend(today, today, "10000");               // 오늘
        stubNetSpend(start, today, "260000");              // 주기 누적

        SpendingGuideSummaryResponse response = service().getSummary(MEMBER_ID);

        assertThat(response.setupRequired()).isFalse();
        assertThat(response.remainingDays()).isEqualTo(20);
        assertThat(response.availableAmount()).isEqualByComparingTo("1000000");
        // (1,000,000 − 250,000) / 20 = 37,500 — 오늘 쓴 10,000은 권장액을 흔들지 않는다
        assertThat(response.dailyRecommendedAmount()).isEqualByComparingTo("37500");
        assertThat(response.todayNetSpend()).isEqualByComparingTo("10000");
        // 오늘 사용 가능 = 37,500 − 10,000 = 27,500
        assertThat(response.todayAvailableAmount()).isEqualByComparingTo("27500");
        assertThat(response.remainingAmount()).isEqualByComparingTo("740000");
        assertThat(response.overCycleBudget()).isFalse();
    }

    @Test
    @DisplayName("예산이 마이너스이고 초과지출한 사용자: 하루 권장액 0원 + 초과금액·경고 플래그를 그대로 노출")
    void negativeBudgetShowsWarnings() {
        LocalDate today = TODAY.value();
        LocalDate start = today.minusDays(5);
        LocalDate end = today.plusDays(19);
        // 월급 200 고정 150 저축 60 → 월 예산 −10만원
        Member member = member(new BigDecimal("2000000"), 5, new BigDecimal("600000"));
        Budget budget = budget(member, start, end, "2000000", "600000", "1500000");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(budgetCycleService.getOrCreateCurrentBudget(any(), any())).willReturn(budget);
        stubNetSpend(start, today.minusDays(1), "0");
        stubNetSpend(today, today, "0");
        stubNetSpend(start, today, "50000");

        SpendingGuideSummaryResponse response = service().getSummary(MEMBER_ID);

        assertThat(response.availableAmount()).isEqualByComparingTo("-100000");
        assertThat(response.dailyRecommendedAmount()).isEqualByComparingTo("0"); // −5,000이 아니라 0
        assertThat(response.todayAvailableAmount()).isEqualByComparingTo("0");
        assertThat(response.remainingAmount()).isEqualByComparingTo("-150000");
        assertThat(response.overCycleBudget()).isTrue();
        assertThat(response.cycleOverspentAmount()).isEqualByComparingTo("150000");
        assertThat(response.budgetUnaffordable()).isTrue();
    }

    @Test
    @DisplayName("월급·급여일 미등록이면 오늘 거래 미리보기도 SPENDING_GUIDE_SETUP_REQUIRED로 거부한다")
    void todayTransactionsRequiresSetup() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member(null, null, null)));

        assertThatThrownBy(() -> service().getTodayTransactions(MEMBER_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPENDING_GUIDE_SETUP_REQUIRED);
    }

    @Test
    @DisplayName("본인 소유가 아니거나 존재하지 않는 budgetId는 404로 거부한다")
    void searchTransactionsRejectsUnknownBudget() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member(new BigDecimal("1000000"), 5, null)));
        given(budgetRepository.findByIdAndMember_Id(99L, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().searchTransactions(MEMBER_ID, 99L, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUDGET_NOT_FOUND);
    }

    @Test
    @DisplayName("아직 시작하지 않은 미래 급여 주기는 400으로 거부한다")
    void searchTransactionsRejectsFutureCycle() {
        LocalDate today = TODAY.value();
        Member member = member(new BigDecimal("1000000"), 5, null);
        Budget futureBudget = budget(member, today.plusDays(10), today.plusDays(39), "1000000", "0", "0");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(budgetRepository.findByIdAndMember_Id(7L, MEMBER_ID)).willReturn(Optional.of(futureBudget));

        assertThatThrownBy(() -> service().searchTransactions(MEMBER_ID, 7L, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUDGET_FUTURE_CYCLE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("카드 연동 사용자의 현재 진행 중인 주기는 completedCycleResult가 없다")
    void categorySummaryForCurrentCycleHasNoResult() {
        LocalDate today = TODAY.value();
        Member member = member(new BigDecimal("1000000"), 5, null);
        member.startCardSync(today.minusMonths(4).atStartOfDay());
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        Budget budget = budget(member, today.minusDays(5), today.plusDays(10), "1000000", "0", "0");
        given(budgetRepository.findByIdAndMember_Id(31L, MEMBER_ID)).willReturn(Optional.of(budget));
        given(cardTransactionRepository.summarizeByCategory(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), any(), any()))
                .willReturn(List.of());
        given(cardTransactionRepository.sumCanceledAmount(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), any(), any()))
                .willReturn(BigDecimal.ZERO);

        CategorySummaryListResponse response = service().getCategorySummary(MEMBER_ID, 31L);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.completedCycleResult()).isNull();
    }

    @Test
    @DisplayName("완료된 과거 주기는 예산·사용·결과 요약을 계산한다")
    void categorySummaryForCompletedCycleComputesResult() {
        LocalDate today = TODAY.value();
        Member member = member(new BigDecimal("1000000"), 5, null);
        member.startCardSync(today.minusMonths(6).atStartOfDay());
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        LocalDate cycleStart = today.minusMonths(2);
        LocalDate cycleEnd = today.minusMonths(1).minusDays(1);
        Budget budget = budget(member, cycleStart, cycleEnd, "1000000", "0", "0");
        given(budgetRepository.findByIdAndMember_Id(31L, MEMBER_ID)).willReturn(Optional.of(budget));
        given(cardTransactionRepository.summarizeByCategory(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), eq(cycleStart), eq(cycleEnd)))
                .willReturn(List.of());
        given(cardTransactionRepository.sumCanceledAmount(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), eq(cycleStart), eq(cycleEnd)))
                .willReturn(BigDecimal.ZERO);
        given(cardTransactionRepository.sumNetSpend(eq(MEMBER_ID), eq(TransactionStatus.REJECTED), eq(cycleStart), eq(cycleEnd)))
                .willReturn(new BigDecimal("300000"));

        CategorySummaryListResponse response = service().getCategorySummary(MEMBER_ID, 31L);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.completedCycleResult()).isNotNull();
        assertThat(response.completedCycleResult().budgetAmount()).isEqualByComparingTo("1000000");
        assertThat(response.completedCycleResult().spentAmount()).isEqualByComparingTo("300000");
        assertThat(response.completedCycleResult().resultAmount()).isEqualByComparingTo("700000");
        assertThat(response.completedCycleResult().resultType()).isEqualTo("REMAINED");
    }

    @Test
    @DisplayName("데이터 보유 시작일보다 이전인 주기는 partialCycle이며 결과 요약을 만들지 않는다")
    void categorySummaryMarksPartialCycle() {
        LocalDate today = TODAY.value();
        Member member = member(new BigDecimal("1000000"), 5, null);
        LocalDate dataStartDate = today.withDayOfMonth(1).minusMonths(3);
        member.startCardSync(today.atStartOfDay());
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        LocalDate cycleStart = dataStartDate.minusMonths(1);
        LocalDate cycleEnd = dataStartDate.minusDays(1);
        Budget budget = budget(member, cycleStart, cycleEnd, "1000000", "0", "0");
        given(budgetRepository.findByIdAndMember_Id(31L, MEMBER_ID)).willReturn(Optional.of(budget));
        given(cardTransactionRepository.summarizeByCategory(any(), any(), any(), any())).willReturn(List.of());
        given(cardTransactionRepository.sumCanceledAmount(any(), any(), any(), any())).willReturn(BigDecimal.ZERO);

        CategorySummaryListResponse response = service().getCategorySummary(MEMBER_ID, 31L);

        assertThat(response.partialCycle()).isTrue();
        assertThat(response.completedCycleResult()).isNull();
    }

    @Test
    @DisplayName("데이터 보유 시작일이 포함된 가장 이른 주기에서는 hasPrevious가 false다")
    void categorySummaryHasPreviousFalseAtEarliestCycle() {
        LocalDate today = TODAY.value();
        Member member = member(new BigDecimal("1000000"), 5, null);
        LocalDate cardSyncStartedAt = today.minusMonths(3);
        member.startCardSync(cardSyncStartedAt.atStartOfDay());
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        LocalDate dataStartDate = BackfillWindowPolicy.calendarFloor(cardSyncStartedAt);
        BudgetCyclePolicy.BudgetCycle earliestCycle = BudgetCyclePolicy.cycleOf(5, dataStartDate);
        Budget budget = budget(member, earliestCycle.cycleStartDate(), earliestCycle.cycleEndDate(), "1000000", "0", "0");
        given(budgetRepository.findByIdAndMember_Id(31L, MEMBER_ID)).willReturn(Optional.of(budget));
        given(cardTransactionRepository.summarizeByCategory(any(), any(), any(), any())).willReturn(List.of());
        given(cardTransactionRepository.sumCanceledAmount(any(), any(), any(), any())).willReturn(BigDecimal.ZERO);

        CategorySummaryListResponse response = service().getCategorySummary(MEMBER_ID, 31L);

        assertThat(response.hasPrevious()).isFalse();
    }

    /** 테스트 시작 시각의 오늘(Asia/Seoul)을 한 번 고정해 예산 주기 경계 계산을 결정적으로 만든다. */
    private static final class ZoneOffsetToday {
        private final LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);

        LocalDate value() {
            return today;
        }
    }
}
