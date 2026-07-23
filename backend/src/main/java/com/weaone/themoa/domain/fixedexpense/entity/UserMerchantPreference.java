package com.weaone.themoa.domain.fixedexpense.entity;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

/**
 * 사용자별 가맹점 설정(erd.md §4). fixedExpense.md §3의 재추천 방지 2겹 중 영구 정책 레이어 —
 * 판정의 진실의 원천이며, {@link FixedExpenseCandidate#getStatus()}는 순수 이력일 뿐이다.
 *
 * <p>거절·습관분류(DO_NOT_RECOMMEND·RECLASSIFY_HABIT)는 {@code recurringPaymentGroup} 단위로 건다 —
 * alias·billerMerchant 단위로 걸면 같은 alias(또는 merchant) 아래 공존하는 별개 구독(예: 같은 서비스를
 * 계정 두 개로 구독)까지 함께 억제돼버린다(fixedExpense.md §2). {@code merchantAlias}·{@code billerMerchant}는
 * CATEGORY_OVERRIDE(erd.md §4 확정 스키마상 존재, MVP 미사용) 전용으로 남겨둔다 — 이건 특정 구독이 아니라
 * 가맹점 신원 자체에 거는 설정이라 group 단위가 아니라 alias/biller 단위가 맞다.
 */
@Entity
@Table(name = "user_merchant_preferences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_merchant_preference_alias",
                        columnNames = {"member_id", "merchant_alias_id", "preference_type"}),
                @UniqueConstraint(name = "uk_user_merchant_preference_biller",
                        columnNames = {"member_id", "biller_merchant_id", "preference_type"}),
                @UniqueConstraint(name = "uk_user_merchant_preference_group",
                        columnNames = {"member_id", "recurring_group_id", "preference_type"})
        })
@Check(constraints = "(case when merchant_alias_id is not null then 1 else 0 end "
        + "+ case when biller_merchant_id is not null then 1 else 0 end "
        + "+ case when recurring_group_id is not null then 1 else 0 end) = 1")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMerchantPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** CATEGORY_OVERRIDE(가맹점 신원 자체에 거는 설정) 전용. 거절·습관분류는 recurringPaymentGroup을 쓴다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    /** CATEGORY_OVERRIDE의 biller 경유분 전용. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_merchant_id")
    private Merchant billerMerchant;

    /** 거절·습관분류 전용 — 이름형·biller형 구분 없이 그룹 하나만 억제한다(위 클래스 설명). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_group_id")
    private RecurringPaymentGroup recurringPaymentGroup;

    /** CATEGORY_OVERRIDE일 때만 채운다 — MVP는 이 값을 저장하지 않으므로 사실상 항상 NULL이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 20)
    private UserMerchantPreferenceType preferenceType;

    private UserMerchantPreference(Member member, MerchantAlias merchantAlias, Merchant billerMerchant,
                                    RecurringPaymentGroup recurringPaymentGroup, Category category,
                                    UserMerchantPreferenceType preferenceType) {
        this.member = member;
        this.merchantAlias = merchantAlias;
        this.billerMerchant = billerMerchant;
        this.recurringPaymentGroup = recurringPaymentGroup;
        this.category = category;
        this.preferenceType = preferenceType;
    }

    /** CATEGORY_OVERRIDE 전용(현재 저장 경로 없음). */
    public static UserMerchantPreference create(Member member, MerchantAlias merchantAlias,
                                                  UserMerchantPreferenceType preferenceType) {
        return new UserMerchantPreference(member, merchantAlias, null, null, null, preferenceType);
    }

    /** CATEGORY_OVERRIDE의 biller 경유분 전용(현재 저장 경로 없음). */
    public static UserMerchantPreference createForBiller(Member member, Merchant billerMerchant,
                                                           UserMerchantPreferenceType preferenceType) {
        return new UserMerchantPreference(member, null, billerMerchant, null, null, preferenceType);
    }

    /** 거절·습관분류 전용({@link com.weaone.themoa.domain.fixedexpense.service.FixedExpenseCandidateService}). */
    public static UserMerchantPreference createForGroup(Member member, RecurringPaymentGroup recurringPaymentGroup,
                                                          UserMerchantPreferenceType preferenceType) {
        return new UserMerchantPreference(member, null, null, recurringPaymentGroup, null, preferenceType);
    }
}
