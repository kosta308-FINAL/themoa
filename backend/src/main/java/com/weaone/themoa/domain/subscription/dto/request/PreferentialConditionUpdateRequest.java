package com.weaone.themoa.domain.subscription.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 관리자 우대조건 수동 수정 요청. 전달된 항목으로 캐시를 통째로 교체하고 잠근다.
 */
public record PreferentialConditionUpdateRequest(
        @NotNull @Valid List<Item> items) {

    public record Item(
            @NotBlank String description,
            BigDecimal rateBonus) {
    }
}
