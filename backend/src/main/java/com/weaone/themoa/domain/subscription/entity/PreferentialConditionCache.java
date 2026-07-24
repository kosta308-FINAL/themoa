package com.weaone.themoa.domain.subscription.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 하나의 우대조건 파싱 결과를 고정 저장하는 캐시.
 *
 * <p>가입 등록 화면의 체크리스트가 매 요청마다 LLM을 새로 호출해 결과가 흔들리던 문제를 없애기 위해,
 * 상품당 한 번 파싱한 결과를 여기 저장하고 이후에는 이 값을 그대로 읽는다.
 *
 * <p>원문({@code special_condition})이 바뀌면 재파싱 대상이 되지만, 관리자가 손본 캐시
 * ({@code editedByAdmin})는 배치가 함부로 덮지 않는다 — 대신 {@code stale}로 "원문 변경됨"만
 * 표시해 관리자가 확인 후 결정하게 한다(사람 수정 > 자동 재파싱).
 */
@Entity
@Table(name = "preferential_condition_cache")
public class PreferentialConditionCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 적금 상품 id(SavingsProduct.id). 상품당 1개라 유니크. */
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    /** 파싱 근거가 된 원문의 해시. 배치에서 원문 변경 여부를 이 값으로 판단한다. */
    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    /** 관리자가 수동으로 수정해 잠근 캐시. true면 배치가 자동으로 덮지 않는다. */
    @Column(name = "edited_by_admin", nullable = false)
    private boolean editedByAdmin;

    /** 잠긴 캐시인데 원문이 바뀐 상태. 관리자에게 "재검토 필요"로 보여주기 위한 표시. */
    @Column(name = "stale", nullable = false)
    private boolean stale;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "cache", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordering ASC")
    private List<PreferentialConditionCacheItem> items = new ArrayList<>();

    protected PreferentialConditionCache() {
    }

    private PreferentialConditionCache(Long productId, String sourceHash, LocalDateTime updatedAt) {
        this.productId = productId;
        this.sourceHash = sourceHash;
        this.updatedAt = updatedAt;
    }

    public static PreferentialConditionCache create(Long productId, String sourceHash, LocalDateTime now) {
        return new PreferentialConditionCache(productId, sourceHash, now);
    }

    /** 파싱 결과로 항목을 통째로 교체한다(자동 파싱 경로). 잠금·stale은 호출측이 별도로 관리한다. */
    public void replaceItems(List<ParsedItem> parsed, String sourceHash, LocalDateTime now) {
        this.items.clear();
        applyItems(parsed);
        this.sourceHash = sourceHash;
        this.updatedAt = now;
    }

    /** 관리자 수동 수정: 항목 교체 + 잠금 + stale 해제. 이후 배치가 덮지 않는다. */
    public void applyAdminEdit(List<ParsedItem> parsed, LocalDateTime now) {
        this.items.clear();
        applyItems(parsed);
        this.editedByAdmin = true;
        this.stale = false;
        this.updatedAt = now;
    }

    /** 잠긴 캐시의 원문이 바뀌었을 때 재검토 표시만 남긴다(항목은 그대로 유지). */
    public void markStale(String newSourceHash, LocalDateTime now) {
        this.stale = true;
        this.sourceHash = newSourceHash;
        this.updatedAt = now;
    }

    private void applyItems(List<ParsedItem> parsed) {
        int order = 0;
        for (ParsedItem p : parsed) {
            this.items.add(PreferentialConditionCacheItem.of(
                    this, p.description(), p.rateBonus(), order++));
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public boolean isEditedByAdmin() {
        return editedByAdmin;
    }

    public boolean isStale() {
        return stale;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<PreferentialConditionCacheItem> getItems() {
        return items;
    }

    /** 캐시에 채워 넣을 항목 한 줄(파싱 결과·관리자 입력 공통). */
    public record ParsedItem(String description, BigDecimal rateBonus) {
    }
}
