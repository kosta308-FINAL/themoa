package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.service.CategoryInsightType;
import com.weaone.themoa.domain.cardtransaction.service.ChangeDirection;
import com.weaone.themoa.domain.cardtransaction.service.CyclePhase;

import java.math.BigDecimal;

public record CategoryInsightResponse(
        CategoryInsightType type,
        ChangeDirection direction,
        BigDecimal amount,
        BigDecimal rate,
        CyclePhase phase,
        BigDecimal percentage,
        Integer cycleCount
) {
}
