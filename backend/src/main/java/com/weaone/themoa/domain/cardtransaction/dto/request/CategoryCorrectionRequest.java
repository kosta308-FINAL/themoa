package com.weaone.themoa.domain.cardtransaction.dto.request;

import jakarta.validation.constraints.NotNull;

/** 거래 건별 카테고리 수정(category.md §2-④). */
public record CategoryCorrectionRequest(
        @NotNull Long categoryId
) {
}
