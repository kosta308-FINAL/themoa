package com.weaone.themoa.domain.category.entity;

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
 * 업종(resMemberStoreType) → 카테고리 매핑(category.md §4). 키워드 규칙(①)이 전부 실패했을 때만 쓰는
 * 보조 신호라 완전일치로만 매칭한다 — 실데이터 241건 중 약 30%가 업종 오염이라 부분일치는 위험하다.
 */
@Entity
@Table(name = "merchant_type_category_map")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantTypeCategoryMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_type", nullable = false, unique = true, length = 100)
    private String merchantType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private MerchantTypeCategoryMap(String merchantType, Category category) {
        this.merchantType = merchantType;
        this.category = category;
    }

    public static MerchantTypeCategoryMap seed(String merchantType, Category category) {
        return new MerchantTypeCategoryMap(merchantType, category);
    }
}
