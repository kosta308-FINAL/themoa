package com.weaone.themoa.domain.coaching.entity;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.member.entity.Member;
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

import java.time.LocalDateTime;

/**
 * 사용자가 코칭 카드를 넘긴 기록(habitExpense.md §5). 사용자당 대상 1행 — 카테고리 UNIQUE와 별칭 UNIQUE를
 * 별도 인덱스 2개로 나눈다(한 인덱스에 두 FK를 묶으면 NULL끼리 충돌하지 않아 중복이 통과한다).
 */
@Entity
@Table(name = "coaching_dismiss",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_coaching_dismiss_category", columnNames = {"member_id", "category_id"}),
                @UniqueConstraint(name = "uk_coaching_dismiss_alias", columnNames = {"member_id", "merchant_alias_id"})
        })
@Check(constraints = "(category_id is not null) <> (merchant_alias_id is not null)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingDismiss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CoachingCardTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    @Enumerated(EnumType.STRING)
    @Column(name = "dismiss_type", nullable = false, length = 20)
    private CoachingDismissType dismissType;

    @Column(name = "dismissed_at", nullable = false)
    private LocalDateTime dismissedAt;

    private CoachingDismiss(Member member, CoachingCardTargetType targetType, Category category,
                             MerchantAlias merchantAlias, CoachingDismissType dismissType, LocalDateTime dismissedAt) {
        this.member = member;
        this.targetType = targetType;
        this.category = category;
        this.merchantAlias = merchantAlias;
        this.dismissType = dismissType;
        this.dismissedAt = dismissedAt;
    }

    public static CoachingDismiss forCategory(Member member, Category category, CoachingDismissType dismissType,
                                               LocalDateTime dismissedAt) {
        return new CoachingDismiss(member, CoachingCardTargetType.CATEGORY, category, null, dismissType, dismissedAt);
    }

    public static CoachingDismiss forMerchantAlias(Member member, MerchantAlias merchantAlias,
                                                    CoachingDismissType dismissType, LocalDateTime dismissedAt) {
        return new CoachingDismiss(member, CoachingCardTargetType.MERCHANT_ALIAS, null, merchantAlias, dismissType,
                dismissedAt);
    }

    /** 이미 넘긴 대상을 다른 유형으로 다시 넘겼을 때의 갱신(예: NOT_WASTE → HIDE). */
    public void updateDismissType(CoachingDismissType dismissType, LocalDateTime dismissedAt) {
        this.dismissType = dismissType;
        this.dismissedAt = dismissedAt;
    }
}
