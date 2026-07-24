package com.weaone.themoa.domain.fixedexpense.entity;

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

import java.time.LocalDateTime;

/**
 * 사용자가 고정지출 코칭 카드를 "다시 보지 않기"로 넘긴 기록. 대상당 1행 — 다음 주기 후보 추출에서
 * 이 고정지출은 영구히 제외된다(LLM이 매번 다시 판단하게 두지 않고 서버가 하드 필터로 뺀다).
 */
@Entity
@Table(name = "fixed_expense_coaching_dismiss",
        uniqueConstraints = @UniqueConstraint(name = "uk_fx_coaching_dismiss",
                columnNames = {"member_id", "fixed_expense_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpenseCoachingDismiss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixed_expense_id", nullable = false)
    private FixedExpense fixedExpense;

    @Column(name = "dismissed_at", nullable = false)
    private LocalDateTime dismissedAt;

    private FixedExpenseCoachingDismiss(Member member, FixedExpense fixedExpense, LocalDateTime dismissedAt) {
        this.member = member;
        this.fixedExpense = fixedExpense;
        this.dismissedAt = dismissedAt;
    }

    public static FixedExpenseCoachingDismiss of(Member member, FixedExpense fixedExpense, LocalDateTime dismissedAt) {
        return new FixedExpenseCoachingDismiss(member, fixedExpense, dismissedAt);
    }
}
