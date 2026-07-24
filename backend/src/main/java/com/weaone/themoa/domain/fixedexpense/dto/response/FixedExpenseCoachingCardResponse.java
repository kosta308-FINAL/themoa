package com.weaone.themoa.domain.fixedexpense.dto.response;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCoachingCard;

import java.math.BigDecimal;

public record FixedExpenseCoachingCardResponse(
        Long id,
        Long fixedExpenseId,
        String title,
        String body,
        BigDecimal annualAmount,
        short displayOrder
) {

    public static FixedExpenseCoachingCardResponse from(FixedExpenseCoachingCard card) {
        return new FixedExpenseCoachingCardResponse(
                card.getId(),
                card.getFixedExpense().getId(),
                card.getTitle(),
                card.getBody(),
                card.getAnnualAmount(),
                card.getDisplayOrder()
        );
    }
}
