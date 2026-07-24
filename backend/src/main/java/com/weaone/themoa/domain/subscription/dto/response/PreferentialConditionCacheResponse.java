package com.weaone.themoa.domain.subscription.dto.response;

import com.weaone.themoa.domain.subscription.entity.PreferentialConditionCache;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자용 우대조건 캐시 응답. 잠금·재검토 상태를 함께 내려 관리자가 판단할 수 있게 한다.
 */
public record PreferentialConditionCacheResponse(
        Long productId,
        boolean editedByAdmin,
        boolean stale,
        LocalDateTime updatedAt,
        List<Item> items) {

    public record Item(String description, BigDecimal rateBonus) {
    }

    public static PreferentialConditionCacheResponse from(PreferentialConditionCache cache) {
        List<Item> items = cache.getItems().stream()
                .map(i -> new Item(i.getDescription(), i.getRateBonus()))
                .toList();
        return new PreferentialConditionCacheResponse(
                cache.getProductId(),
                cache.isEditedByAdmin(),
                cache.isStale(),
                cache.getUpdatedAt(),
                items);
    }
}
