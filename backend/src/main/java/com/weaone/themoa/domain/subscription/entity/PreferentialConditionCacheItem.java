package com.weaone.themoa.domain.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 상품 우대조건 파싱 캐시의 개별 항목(체크리스트 한 줄).
 *
 * <p>{@link PreferentialConditionCache}에 종속된다. 순서({@code ordering})까지 저장해 화면에서
 * 매번 같은 순서로 보이게 한다.
 */
@Entity
@Table(name = "preferential_condition_cache_item")
public class PreferentialConditionCacheItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cache_id", nullable = false)
    private PreferentialConditionCache cache;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /** 이 조건 하나가 주는 가산금리(%p). 원문에 개별 값이 없으면 0. */
    @Column(name = "rate_bonus", precision = 6, scale = 3)
    private BigDecimal rateBonus;

    @Column(name = "ordering", nullable = false)
    private int ordering;

    protected PreferentialConditionCacheItem() {
    }

    private PreferentialConditionCacheItem(PreferentialConditionCache cache, String description,
                                           BigDecimal rateBonus, int ordering) {
        this.cache = cache;
        this.description = description;
        this.rateBonus = rateBonus;
        this.ordering = ordering;
    }

    static PreferentialConditionCacheItem of(PreferentialConditionCache cache, String description,
                                             BigDecimal rateBonus, int ordering) {
        return new PreferentialConditionCacheItem(cache, description, rateBonus, ordering);
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getRateBonus() {
        return rateBonus;
    }

    public int getOrdering() {
        return ordering;
    }
}
