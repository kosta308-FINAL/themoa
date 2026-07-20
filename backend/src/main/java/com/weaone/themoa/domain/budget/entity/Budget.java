package com.weaone.themoa.domain.budget.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 주기별 예산 스냅샷(dailyBudget.md, erd.md §6). "입력값(월급·저축목표·고정지출 합계)"만 저장하고
 * 월 예산·하루 권장액 같은 파생값은 컬럼으로 두지 않는다 — 전부 조회 시 계산이다. 파생 컬럼이 없어야
 * 월급 "이번 주기부터 적용" 시 {@code salaryAmount}만 UPDATE하면 월 예산·하루 권장액이 자동으로 따라온다.
 *
 * <p>{@code cycleStartDate}·{@code cycleEndDate}는 생성 시점 {@code member.payday}로 계산해 저장한 뒤
 * 어떤 사유로도 UPDATE하지 않는다(현재 주기 포함). 완료된 과거 주기 집계가 급여일 재계산에 흔들리지 않게 한다.
 */
@Entity
@Table(name = "budget",
        uniqueConstraints = @UniqueConstraint(name = "uk_budget_member_cycle",
                columnNames = {"member_id", "\"year_month\""}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 월급 주기 라벨("yyyy-MM" = 월급이 시작된 달). 표시용이며 실제 날짜 범위는 {@code cycleStartDate}·
     * {@code cycleEndDate}가 정본이다(dailyBudget.md §1). {@code year_month}는 MySQL 예약어라 인용한다.
     */
    @Column(name = "\"year_month\"", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "cycle_start_date", nullable = false)
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date", nullable = false)
    private LocalDate cycleEndDate;

    /** 그 주기 월급 스냅샷. 과거 주기 행은 불변, 현재 주기는 "이번 주기부터 적용" 시에만 UPDATE. */
    @Column(name = "salary_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal salaryAmount;

    @Column(name = "savings_goal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal savingsGoalAmount;

    /** 고정지출 예상합계 = 규칙 expected_amount_krw 합(원화). expected_amount 합이 아니다(통화 혼재). */
    @Column(name = "expected_fixed_expense_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedFixedExpenseTotal;

    /** 실제 이행 합계(참고용). 주기 시작 시점엔 결제 전이라 0이다. */
    @Column(name = "confirmed_fixed_expense_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal confirmedFixedExpenseTotal;

    private Budget(Member member, String yearMonth, LocalDate cycleStartDate, LocalDate cycleEndDate,
                   BigDecimal salaryAmount, BigDecimal savingsGoalAmount, BigDecimal expectedFixedExpenseTotal,
                   BigDecimal confirmedFixedExpenseTotal) {
        this.member = member;
        this.yearMonth = yearMonth;
        this.cycleStartDate = cycleStartDate;
        this.cycleEndDate = cycleEndDate;
        this.salaryAmount = salaryAmount;
        this.savingsGoalAmount = savingsGoalAmount;
        this.expectedFixedExpenseTotal = expectedFixedExpenseTotal;
        this.confirmedFixedExpenseTotal = confirmedFixedExpenseTotal;
    }

    public static Budget openCycle(Member member, String yearMonth, LocalDate cycleStartDate, LocalDate cycleEndDate,
                                   BigDecimal salaryAmount, BigDecimal savingsGoalAmount,
                                   BigDecimal expectedFixedExpenseTotal, BigDecimal confirmedFixedExpenseTotal) {
        return new Budget(member, yearMonth, cycleStartDate, cycleEndDate, salaryAmount, savingsGoalAmount,
                expectedFixedExpenseTotal, confirmedFixedExpenseTotal);
    }

    /** "이번 주기부터 적용" 월급 변경(MOA-S-BUD-BGT-08). 파생 컬럼이 없어 스냅샷만 바꾸면 예산이 따라온다. */
    public void applySalary(BigDecimal salaryAmount) {
        this.salaryAmount = salaryAmount;
    }

    /** "이번 주기부터 적용" 저축 목표 변경. */
    public void applySavingsGoal(BigDecimal savingsGoalAmount) {
        this.savingsGoalAmount = savingsGoalAmount;
    }

    /**
     * 월 예산 — 음수를 그대로 흘린다(초과·과다 저축목표 정보를 잃지 않기 위해). {@code incomeAdjustmentTotal}은
     * "수입 직접 입력"(용돈·정부지원금 등 비정기 수입, budget_income_adjustment) 합계로, 호출자가 조회해
     * 넘긴다 — 이 엔티티는 순수 계산만 하고 직접 DB에 접근하지 않는다.
     */
    public BigDecimal getAvailableAmount(BigDecimal incomeAdjustmentTotal) {
        return salaryAmount.subtract(expectedFixedExpenseTotal).subtract(savingsGoalAmount).add(incomeAdjustmentTotal);
    }

    /** 남은 예산 — 음수 그대로. spentThisCycle은 주기 시작~오늘의 순지출 합계. */
    public BigDecimal getRemainingAmount(BigDecimal spentThisCycle, BigDecimal incomeAdjustmentTotal) {
        return getAvailableAmount(incomeAdjustmentTotal).subtract(spentThisCycle);
    }

    /**
     * 하루 권장액 = (월 예산 − 어제까지 누적 순지출) ÷ 오늘 포함 남은 일수. 화면에 내보내는 값이라
     * 여기서만 {@code max(0)} 바닥을 건다("−4,000원까지 쓰세요"는 정보가 0). 중간 계산엔 바닥을 걸지 않는다.
     */
    public BigDecimal getDailyRecommendedAmount(BigDecimal spentThroughYesterday, int remainingDays,
                                                 BigDecimal incomeAdjustmentTotal) {
        return getAvailableAmount(incomeAdjustmentTotal)
                .subtract(spentThroughYesterday)
                .divide(BigDecimal.valueOf(remainingDays), 0, RoundingMode.DOWN)
                .max(BigDecimal.ZERO);
    }
}
