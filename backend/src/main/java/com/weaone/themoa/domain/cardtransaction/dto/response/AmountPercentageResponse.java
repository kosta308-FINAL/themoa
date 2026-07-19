package com.weaone.themoa.domain.cardtransaction.dto.response;

import java.math.BigDecimal;

/** 카테고리 소비 상세(categoryDetail.md §10)에서 phase·dayType이 공유하는 금액/비율 한 쌍. */
public record AmountPercentageResponse(
        BigDecimal amount,
        BigDecimal percentage
) {
}
