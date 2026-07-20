package com.weaone.themoa.domain.category.dto.response;

import com.weaone.themoa.domain.category.entity.Category;

/** 거래 입력·수정용 카테고리 목록 항목(dayguide.md §8.1). */
public record CategoryResponse(
        Long id,
        String code,
        String name
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getCode(), category.getName());
    }
}
