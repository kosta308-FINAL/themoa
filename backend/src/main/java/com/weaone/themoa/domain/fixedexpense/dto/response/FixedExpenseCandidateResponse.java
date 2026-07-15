package com.weaone.themoa.domain.fixedexpense.dto.response;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;

import java.math.BigDecimal;

public record FixedExpenseCandidateResponse(
        Long id,
        Long recurringGroupId,
        String merchantAliasName,
        BigDecimal avgAmount,
        short avgPayDay,
        Long recommendedCategoryId,
        String recommendMessage,
        String status
) {

    public static FixedExpenseCandidateResponse from(FixedExpenseCandidate candidate) {
        RecurringPaymentGroup group = candidate.getRecurringPaymentGroup();
        return new FixedExpenseCandidateResponse(
                candidate.getId(),
                group.getId(),
                group.getMerchantAlias().getCanonicalServiceName(),
                group.getAvgAmount(),
                group.getAvgPayDay(),
                candidate.getRecommendedCategory().getId(),
                candidate.getRecommendMessage(),
                candidate.getStatus().name()
        );
    }
}
