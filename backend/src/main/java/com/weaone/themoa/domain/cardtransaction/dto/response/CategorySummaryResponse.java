package com.weaone.themoa.domain.cardtransaction.dto.response;

import java.math.BigDecimal;

public record CategorySummaryResponse(
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        long transactionCount,
        BigDecimal percentage
) {
}
