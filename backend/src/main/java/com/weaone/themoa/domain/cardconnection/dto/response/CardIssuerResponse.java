package com.weaone.themoa.domain.cardconnection.dto.response;

import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.support.CardIssuerPolicy;

/** S-00C 지원 카드사 목록 항목(dayguide.md §8.1). {@code requiresCardCredentials}는 입력 필드 분기용이다. */
public record CardIssuerResponse(
        String organization,
        String name,
        boolean requiresCardCredentials
) {

    public static CardIssuerResponse from(CardIssuer cardIssuer) {
        return new CardIssuerResponse(
                cardIssuer.getOrganization(),
                cardIssuer.getName(),
                CardIssuerPolicy.requiresCardCredentials(cardIssuer.getOrganization())
        );
    }
}
