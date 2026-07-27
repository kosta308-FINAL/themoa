package com.weaone.themoa.domain.recommend.entity;

import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.recommend.service.PreferredPeriod;
import com.weaone.themoa.domain.recommend.service.RiskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 마지막으로 사용한 맞춤 금융상품 추천 조건.
 *
 * <p>나이·월소득·월 납입가능금액은 화면 진입 때 최신 회원·예산 데이터로 다시 계산하므로 저장하지 않는다.
 */
@Entity
@Table(
        name = "financial_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_financial_profile_member",
                columnNames = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "employment_type", nullable = false, length = 20)
    private String employmentType;

    @Column(name = "low_income", nullable = false)
    private boolean lowIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_type", nullable = false, length = 20)
    private RiskType riskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_period", nullable = false, length = 10)
    private PreferredPeriod preferredPeriod;

    @Column(name = "accept_condition", nullable = false)
    private boolean acceptCondition;

    @Column(name = "need_liquidity", nullable = false)
    private boolean needLiquidity;

    @Column(name = "goal_amount_won")
    private Integer goalAmountWon;

    @Column(name = "goal_months")
    private Integer goalMonths;

    private FinancialProfile(Member member) {
        this.member = member;
    }

    public static FinancialProfile create(Member member) {
        return new FinancialProfile(member);
    }

    public void update(String employmentType,
                       boolean lowIncome,
                       RiskType riskType,
                       PreferredPeriod preferredPeriod,
                       boolean acceptCondition,
                       boolean needLiquidity,
                       Integer goalAmountWon,
                       Integer goalMonths) {
        this.employmentType = employmentType;
        this.lowIncome = lowIncome;
        this.riskType = riskType;
        this.preferredPeriod = preferredPeriod;
        this.acceptCondition = acceptCondition;
        this.needLiquidity = needLiquidity;
        this.goalAmountWon = goalAmountWon;
        this.goalMonths = goalAmountWon == null ? null : goalMonths;
    }
}
