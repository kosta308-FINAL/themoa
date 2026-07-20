package com.weaone.themoa.domain.budget.dto.response;

import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;

import java.util.List;

/** S-01 오늘 거래 미리보기(dayguide.md §8.1). {@code totalCount}는 {@code limit} 적용 전 전체 건수다. */
public record TodayTransactionsResponse(
        List<CardTransactionResponse> items,
        long totalCount
) {
}
