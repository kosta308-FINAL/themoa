package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.budget.service.SpendingGuideService;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.coaching.dto.response.CoachingCardListResponse;
import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import com.weaone.themoa.domain.coaching.repository.CoachingCardRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** 소비 가이드 코칭 카드 조회(dayguide.md §3.5·§8.1) 검증. */
@ExtendWith(MockitoExtension.class)
class CoachingCardQueryServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private CoachingCardRepository coachingCardRepository;
    @Mock
    private SpendingGuideService spendingGuideService;
    @Mock
    private BudgetCycleService budgetCycleService;

    @InjectMocks
    private CoachingCardQueryService coachingCardQueryService;

    private Member member(int payday) {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now());
        member.configureSpendingGuide(IncomeType.SALARY, BigDecimal.valueOf(3_000_000), null, payday);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private Budget currentBudget(Member member) {
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        BudgetCyclePolicy.BudgetCycle cycle = BudgetCyclePolicy.cycleOf(member.getPayday(), today);
        return Budget.openCycle(member, cycle.yearMonth(), cycle.cycleStartDate(), cycle.cycleEndDate(),
                member.getSalaryAmount(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private String analyzedYearMonth(Member member, Budget budget) {
        BudgetCyclePolicy.BudgetCycle current = BudgetCyclePolicy.cycleOf(member.getPayday(), budget.getCycleStartDate());
        BudgetCyclePolicy.BudgetCycle previous =
                BudgetCyclePolicy.cycleOf(member.getPayday(), current.cycleStartDate().minusDays(1));
        given(budgetCycleService.previousCompletedCycle(member, budget.getCycleStartDate())).willReturn(previous);
        return previous.yearMonth();
    }

    @Test
    @DisplayName("직전 완료 주기 카드가 있으면 emptyReason 없이 그대로 반환한다")
    void returnsCardsWithoutEmptyReason() {
        Member member = member(5);
        Budget budget = currentBudget(member);
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, null)).willReturn(budget);
        String yearMonth = analyzedYearMonth(member, budget);
        Category category = Category.seed(CategoryCode.FOOD, "식비");
        CoachingCard card = CoachingCard.forCategory(member, yearMonth, "제목", "본문", category,
                BigDecimal.valueOf(10000), (short) 1, LocalDateTime.now());
        given(coachingCardRepository.findByMember_IdAndYearMonthAndDismissedAtIsNullOrderByDisplayOrderAsc(MEMBER_ID, yearMonth))
                .willReturn(List.of(card));

        CoachingCardListResponse response = coachingCardQueryService.list(MEMBER_ID, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.emptyReason()).isNull();
    }

    @Test
    @DisplayName("카드 미연동 회원은 카드가 없으면 CARD_NOT_CONNECTED를 반환한다")
    void returnsCardNotConnectedForManualMode() {
        Member member = member(5);
        Budget budget = currentBudget(member);
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, null)).willReturn(budget);
        given(coachingCardRepository.findByMember_IdAndYearMonthAndDismissedAtIsNullOrderByDisplayOrderAsc(MEMBER_ID, analyzedYearMonth(member, budget)))
                .willReturn(List.of());

        CoachingCardListResponse response = coachingCardQueryService.list(MEMBER_ID, null);

        assertThat(response.items()).isEmpty();
        assertThat(response.emptyReason()).isEqualTo("CARD_NOT_CONNECTED");
    }

    @Test
    @DisplayName("카드 연동 회원이지만 분석할 카드가 없으면 INSUFFICIENT_DATA를 반환한다")
    void returnsInsufficientDataForCardModeWithoutCards() {
        Member member = member(5);
        member.startCardSync(LocalDateTime.now());
        Budget budget = currentBudget(member);
        given(spendingGuideService.getBudgetOrCurrent(MEMBER_ID, 31L)).willReturn(budget);
        given(coachingCardRepository.findByMember_IdAndYearMonthAndDismissedAtIsNullOrderByDisplayOrderAsc(MEMBER_ID, analyzedYearMonth(member, budget)))
                .willReturn(List.of());

        CoachingCardListResponse response = coachingCardQueryService.list(MEMBER_ID, 31L);

        assertThat(response.items()).isEmpty();
        assertThat(response.emptyReason()).isEqualTo("INSUFFICIENT_DATA");
    }
}
