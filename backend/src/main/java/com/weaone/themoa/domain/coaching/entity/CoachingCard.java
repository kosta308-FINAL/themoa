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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 습관성 지출 코칭 카드(habitExpense.md §5, erd.md). 주기별 배치가 만들어 소비 가이드 화면에 최대 3장
 * 표시한다. 코칭 대상은 카테고리 또는 가맹점 별칭 중 배타적으로 하나만 참조한다(이름 문자열로 담지 않는 이유는
 * {@code category.name}이 자유 변경 컬럼이라 표시명이 바뀌면 {@link CoachingDismiss} 기록이 끊기기 때문).
 */
@Entity
@Table(name = "coaching_card",
        uniqueConstraints = @UniqueConstraint(name = "uk_coaching_card_display_order",
                columnNames = {"member_id", "year_month", "display_order"}))
@Check(constraints = "(category_id is not null) <> (merchant_alias_id is not null)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 분석 대상이 된 급여 주기 라벨(코칭 카드가 만들어진 근거 주기). 배치 멱등성 키의 일부다. */
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    /** 문장은 전부 AI(또는 폴백 템플릿)가 쓴다. */
    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CoachingCardTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    /** 숫자는 전부 규칙 계층이 계산한다 — LLM 반환값을 쓰지 않는다(habitExpense.md §4). */
    @Column(name = "estimated_saving", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedSaving;

    /** 주기 내 표시 순서 1~3. */
    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private CoachingCard(Member member, String yearMonth, String title, String body,
                          CoachingCardTargetType targetType, Category category, MerchantAlias merchantAlias,
                          BigDecimal estimatedSaving, short displayOrder, LocalDateTime createdAt) {
        this.member = member;
        this.yearMonth = yearMonth;
        this.title = title;
        this.body = body;
        this.targetType = targetType;
        this.category = category;
        this.merchantAlias = merchantAlias;
        this.estimatedSaving = estimatedSaving;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }

    public static CoachingCard forCategory(Member member, String yearMonth, String title, String body,
                                            Category category, BigDecimal estimatedSaving, short displayOrder,
                                            LocalDateTime createdAt) {
        return new CoachingCard(member, yearMonth, title, body, CoachingCardTargetType.CATEGORY, category, null,
                estimatedSaving, displayOrder, createdAt);
    }

    public static CoachingCard forMerchantAlias(Member member, String yearMonth, String title, String body,
                                                 MerchantAlias merchantAlias, BigDecimal estimatedSaving,
                                                 short displayOrder, LocalDateTime createdAt) {
        return new CoachingCard(member, yearMonth, title, body, CoachingCardTargetType.MERCHANT_ALIAS, null,
                merchantAlias, estimatedSaving, displayOrder, createdAt);
    }
}
