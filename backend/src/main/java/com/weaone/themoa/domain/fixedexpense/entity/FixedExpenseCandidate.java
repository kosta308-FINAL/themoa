package com.weaone.themoa.domain.fixedexpense.entity;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 고정지출 등록 추천 후보(fixedExpense.md §3). 그룹당 후보는 1행이며(UNIQUE recurring_group_id),
 * 주기마다 새로 INSERT되지 않고 이 행의 status·snoozedYearMonth가 바뀌며 산다.
 */
@Entity
@Table(name = "fixed_expense_candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpenseCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurring_group_id", nullable = false, unique = true)
    private RecurringPaymentGroup recurringPaymentGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommended_category_id", nullable = false)
    private Category recommendedCategory;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "recommend_message", columnDefinition = "TEXT")
    private String recommendMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FixedExpenseCandidateStatus status;

    /** "나중에" 누른 시점의 월급주기(라벨). 현재 주기가 이 값을 넘어서야 재추천한다(§3). */
    @Column(name = "snoozed_year_month", length = 7)
    private String snoozedYearMonth;

    private FixedExpenseCandidate(Member member, RecurringPaymentGroup recurringPaymentGroup,
                                   Category recommendedCategory, BigDecimal score, String recommendMessage) {
        this.member = member;
        this.recurringPaymentGroup = recurringPaymentGroup;
        this.recommendedCategory = recommendedCategory;
        this.score = score;
        this.recommendMessage = recommendMessage;
        this.status = FixedExpenseCandidateStatus.PENDING;
    }

    public static FixedExpenseCandidate create(Member member, RecurringPaymentGroup recurringPaymentGroup,
                                                Category recommendedCategory, BigDecimal score,
                                                String recommendMessage) {
        return new FixedExpenseCandidate(member, recurringPaymentGroup, recommendedCategory, score, recommendMessage);
    }

    /** 탐지 배치가 최신 그룹 통계로 추천 문구·점수를 갱신할 때 사용한다(재INSERT 아님). */
    public void refresh(Category recommendedCategory, BigDecimal score, String recommendMessage) {
        this.recommendedCategory = recommendedCategory;
        this.score = score;
        this.recommendMessage = recommendMessage;
    }

    public void reopen() {
        this.status = FixedExpenseCandidateStatus.PENDING;
        this.snoozedYearMonth = null;
    }

    public void snooze(String currentYearMonth) {
        this.status = FixedExpenseCandidateStatus.EXCLUDED_THIS_MONTH;
        this.snoozedYearMonth = currentYearMonth;
    }

    public void reject() {
        this.status = FixedExpenseCandidateStatus.DO_NOT_RECOMMEND;
    }

    public void classifyHabit() {
        this.status = FixedExpenseCandidateStatus.CLASSIFIED_HABIT;
    }

    public void register() {
        this.status = FixedExpenseCandidateStatus.REGISTERED;
    }

    /** 현재 주기 > 스누즈 주기(둘 다 "yyyy-MM"이라 사전순 비교가 곧 시간순 비교다). */
    public boolean isSnoozeExpired(String currentYearMonth) {
        return snoozedYearMonth != null && currentYearMonth.compareTo(snoozedYearMonth) > 0;
    }
}
