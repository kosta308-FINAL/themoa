package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.domain.budget.dto.response.SpendingGuideSummaryResponse;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private BudgetCycleService budgetCycleService;
    @Mock
    private CardTransactionRepository cardTransactionRepository;

    private SpendingGuideService service() {
        return new SpendingGuideService(memberRepository, budgetCycleService, cardTransactionRepository);
    }

    private Member member(BigDecimal salary, Integer payday, BigDecimal savingsTarget) {
        Member member = Member.signUp("u@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
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

    /** 테스트 시작 시각의 오늘(Asia/Seoul)을 한 번 고정해 예산 주기 경계 계산을 결정적으로 만든다. */
    private static final class ZoneOffsetToday {
        private final LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);

        LocalDate value() {
            return today;
        }
    }
}
