package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.entity.PaydayChangeHistory;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.repository.PaydayChangeHistoryRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseKrwConverter;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.repository.MemberWorkScheduleRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/** 급여일 변경 승격·이력 기반 주기 조회(payday.md §급여일 변경) 검증. */
@ExtendWith(MockitoExtension.class)
class BudgetCycleServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private FixedExpenseRepository fixedExpenseRepository;
    @Mock
    private FixedExpensePaymentRepository fixedExpensePaymentRepository;
    @Mock
    private FixedExpenseKrwConverter krwConverter;
    @Mock
    private MemberWorkScheduleRepository memberWorkScheduleRepository;
    @Mock
    private WorkScheduleSalaryCalculator workScheduleSalaryCalculator;
    @Mock
    private PaydayChangeHistoryRepository paydayChangeHistoryRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private BudgetCycleService budgetCycleService;

    private Member member(Integer payday) {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now());
        member.configureSpendingGuide(IncomeType.SALARY, BigDecimal.valueOf(3_000_000), null, payday);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    @Test
    @DisplayName("이력이 없으면 payday로 그냥 cycleOf 계산한 것과 같다")
    void resolveCycleForDateWithoutHistoryMatchesCycleOf() {
        Member member = member(5);
        given(paydayChangeHistoryRepository.findByMember_IdOrderByEffectiveCycleStartDateAsc(MEMBER_ID))
                .willReturn(List.of());

        BudgetCyclePolicy.BudgetCycle resolved =
                budgetCycleService.resolveCycleForDate(member, LocalDate.of(2026, 7, 11));

        assertThat(resolved).isEqualTo(BudgetCyclePolicy.cycleOf(5, LocalDate.of(2026, 7, 11)));
    }

    @Test
    @DisplayName("소비 가이드 최초 설정 전(payday=null)이면 NPE 대신 달력 월로 폴백한다")
    void resolveCycleForDateFallsBackToCalendarMonthWhenPaydayIsNull() {
        Member member = member(null);
        given(paydayChangeHistoryRepository.findByMember_IdOrderByEffectiveCycleStartDateAsc(MEMBER_ID))
                .willReturn(List.of());

        BudgetCyclePolicy.BudgetCycle resolved =
                budgetCycleService.resolveCycleForDate(member, LocalDate.of(2026, 7, 11));

        assertThat(resolved.yearMonth()).isEqualTo("2026-07");
    }

    @Test
    @DisplayName("변경 이력이 있으면 그 시점 이전 날짜는 예전 payday, 이후는 새 payday·브리지 주기로 계산한다")
    void resolveCycleForDateHonorsHistory() {
        Member member = member(20); // 승격 후 현재 payday
        PaydayChangeHistory history = PaydayChangeHistory.record(member, 5, 20,
                LocalDate.of(2026, 7, 6), LocalDateTime.now());
        given(paydayChangeHistoryRepository.findByMember_IdOrderByEffectiveCycleStartDateAsc(MEMBER_ID))
                .willReturn(List.of(history));

        // 변경 전 날짜(6/20)는 옛 payday(5)로 계산
        BudgetCyclePolicy.BudgetCycle before =
                budgetCycleService.resolveCycleForDate(member, LocalDate.of(2026, 6, 20));
        assertThat(before).isEqualTo(BudgetCyclePolicy.cycleOf(5, LocalDate.of(2026, 6, 20)));

        // 브리지 주기 안(7/6~7/19)은 브리지 그대로
        BudgetCyclePolicy.BudgetCycle inBridge =
                budgetCycleService.resolveCycleForDate(member, LocalDate.of(2026, 7, 10));
        assertThat(inBridge.cycleStartDate()).isEqualTo(LocalDate.of(2026, 7, 6));
        assertThat(inBridge.cycleEndDate()).isEqualTo(LocalDate.of(2026, 7, 19));

        // 브리지 이후(8월)는 새 payday(20)로 정상 계산
        BudgetCyclePolicy.BudgetCycle after =
                budgetCycleService.resolveCycleForDate(member, LocalDate.of(2026, 8, 25));
        assertThat(after).isEqualTo(BudgetCyclePolicy.cycleOf(20, LocalDate.of(2026, 8, 25)));
    }

    @Test
    @DisplayName("진행 중인 주기의 Budget row가 이미 있으면 예약된 급여일을 승격하지 않는다")
    void doesNotPromoteWhileCurrentCycleStillOpen() {
        Member member = member(5);
        ReflectionTestUtils.setField(member, "pendingPayday", 20);
        given(budgetRepository.findByMember_IdAndYearMonth(MEMBER_ID, "2026-07"))
                .willReturn(Optional.of(Budget.openCycle(member, "2026-07", LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 8, 4), BigDecimal.valueOf(3_000_000), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO)));

        Optional<BudgetCyclePolicy.BudgetCycle> bridge =
                budgetCycleService.ensurePaydayPromoted(member, LocalDate.of(2026, 7, 11));

        assertThat(bridge).isEmpty();
        assertThat(member.getPayday()).isEqualTo(5);
        assertThat(member.getPendingPayday()).isEqualTo(20);
    }

    @Test
    @DisplayName("다음 주기가 열리는 시점이면 예약된 급여일을 승격하고 브리지 주기·이력을 남긴다")
    void promotesWhenNextCycleOpens() {
        Member member = member(5);
        ReflectionTestUtils.setField(member, "pendingPayday", 20);
        given(budgetRepository.findByMember_IdAndYearMonth(MEMBER_ID, "2026-08")).willReturn(Optional.empty());
        given(budgetRepository.findFirstByMember_IdOrderByCycleStartDateDesc(MEMBER_ID))
                .willReturn(Optional.of(Budget.openCycle(member, "2026-07", LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 8, 4), BigDecimal.valueOf(3_000_000), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO)));

        Optional<BudgetCyclePolicy.BudgetCycle> bridge =
                budgetCycleService.ensurePaydayPromoted(member, LocalDate.of(2026, 8, 10));

        assertThat(bridge).isPresent();
        assertThat(bridge.get().cycleStartDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(bridge.get().cycleEndDate()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(member.getPayday()).isEqualTo(20);
        assertThat(member.getPendingPayday()).isNull();
        org.mockito.BDDMockito.then(paydayChangeHistoryRepository).should().save(any());
    }
}
