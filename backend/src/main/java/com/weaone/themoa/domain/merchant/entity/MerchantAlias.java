package com.weaone.themoa.domain.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    /**
     * category 도메인이 아직 구현되지 않아 실제 FK 제약은 걸지 않는다(erd.md는 FK NULL → category).
     * category.md 구현 시 연관관계로 승격한다.
     */
    @Column(name = "default_category_id")
    private Long defaultCategoryId;

    private MerchantAlias(String canonicalServiceName, Long defaultCategoryId) {
        this.canonicalServiceName = canonicalServiceName;
        this.defaultCategoryId = defaultCategoryId;
    }

    public static MerchantAlias create(String canonicalServiceName, Long defaultCategoryId) {
        return new MerchantAlias(canonicalServiceName, defaultCategoryId);
    }
}
