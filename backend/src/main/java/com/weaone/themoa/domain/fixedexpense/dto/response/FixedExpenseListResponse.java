package com.weaone.themoa.domain.fixedexpense.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** F-01 상단 요약("이번 달 고정지출 N건 / 합계") + 목록. */
public record FixedExpenseListResponse(
        int count,
        BigDecimal totalExpectedAmountKrw,
        List<FixedExpenseResponse> items
) {

    public static FixedExpenseListResponse from(List<FixedExpenseResponse> items) {
        BigDecimal total = items.stream()
                .map(FixedExpenseResponse::expectedAmountKrw)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FixedExpenseListResponse(items.size(), total, items);
    }
}
