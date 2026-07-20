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
 * biller 후보는 승인 전까지 alias가 없어 {@code billerMerchant} 기준으로도 걸 수 있다
 * (troubleshooting/billerProblem.md).
 */
@Entity
@Table(name = "user_merchant_preferences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_merchant_preference_alias",
                        columnNames = {"member_id", "merchant_alias_id", "preference_type"}),
                @UniqueConstraint(name = "uk_user_merchant_preference_biller",
                        columnNames = {"member_id", "biller_merchant_id", "preference_type"})
        })
@Check(constraints = "(merchant_alias_id is not null) <> (biller_merchant_id is not null)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMerchantPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 이름형 대상만 채운다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    /** biller형 대상만 채운다(alias 없는 biller 후보의 거절·습관분류 기준). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_merchant_id")
    private Merchant billerMerchant;

    /** CATEGORY_OVERRIDE일 때만 채운다 — MVP는 이 값을 저장하지 않으므로 사실상 항상 NULL이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 20)
    private UserMerchantPreferenceType preferenceType;

    private UserMerchantPreference(Member member, MerchantAlias merchantAlias, Merchant billerMerchant,
                                    Category category, UserMerchantPreferenceType preferenceType) {
        this.member = member;
        this.merchantAlias = merchantAlias;
        this.billerMerchant = billerMerchant;
        this.category = category;
        this.preferenceType = preferenceType;
    }

    public static UserMerchantPreference create(Member member, MerchantAlias merchantAlias,
                                                  UserMerchantPreferenceType preferenceType) {
        return new UserMerchantPreference(member, merchantAlias, null, null, preferenceType);
    }

    public static UserMerchantPreference createForBiller(Member member, Merchant billerMerchant,
                                                           UserMerchantPreferenceType preferenceType) {
        return new UserMerchantPreference(member, null, billerMerchant, null, preferenceType);
    }
}
