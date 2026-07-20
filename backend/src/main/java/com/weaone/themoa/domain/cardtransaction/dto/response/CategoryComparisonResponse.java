package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.service.ChangeDirection;

import java.math.BigDecimal;

public record CategoryComparisonResponse(
        Long categoryId,
        String categoryName,
        BigDecimal selectedAmount,
        BigDecimal previousAmount,
        BigDecimal selectedShare,
        BigDecimal changeAmount,
        BigDecimal changeRate,
        ChangeDirection changeStatus
) {
}
