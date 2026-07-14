package com.weaone.themoa.domain.merchant.entity;

import com.weaone.themoa.domain.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 논리적 "서비스" 단위(예: Claude 구독). 이름 표기 흔들림은 {@link MerchantAliasTerms}가 흡수하고,
 * 상품 티어는 엔티티화하지 않는다(merchant.md §1).
 */
@Entity
@Table(name = "merchant_alias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_service_name", nullable = false, unique = true, length = 255)
    private String canonicalServiceName;

    /** "이 서비스는 보통 이 카테고리"의 초기값 재료(category.md §1). 거래 저장 시 확정 스냅샷과는 별개다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_category_id")
    private Category defaultCategory;

    private MerchantAlias(String canonicalServiceName, Category defaultCategory) {
        this.canonicalServiceName = canonicalServiceName;
        this.defaultCategory = defaultCategory;
    }

    public static MerchantAlias create(String canonicalServiceName, Category defaultCategory) {
        return new MerchantAlias(canonicalServiceName, defaultCategory);
    }
}
