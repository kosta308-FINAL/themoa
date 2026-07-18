package com.weaone.themoa.domain.coaching.dto.response;

import com.weaone.themoa.domain.coaching.entity.CoachingCard;

import java.math.BigDecimal;

public record CoachingCardResponse(
        Long id,
        String yearMonth,
        String title,
        String body,
        String targetType,
        String targetLabel,
        BigDecimal estimatedSaving,
        short displayOrder
) {

    public static CoachingCardResponse from(CoachingCard card) {
        String targetLabel = card.getCategory() != null
                ? card.getCategory().getName()
                : card.getMerchantAlias().getCanonicalServiceName();
        return new CoachingCardResponse(
                card.getId(),
                card.getYearMonth(),
                card.getTitle(),
                card.getBody(),
                card.getTargetType().name(),
                targetLabel,
                card.getEstimatedSaving(),
                card.getDisplayOrder()
        );
    }
}
