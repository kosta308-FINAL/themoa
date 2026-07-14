package com.weaone.themoa.domain.cardconnection.dto.response;

import com.weaone.themoa.domain.cardconnection.entity.CardConnection;

import java.time.LocalDateTime;

public record CardConnectionResponse(
        Long id,
        String organization,
        String organizationName,
        String status,
        LocalDateTime createdAt
) {

    public static CardConnectionResponse from(CardConnection connection) {
        return new CardConnectionResponse(
                connection.getId(),
                connection.getCardIssuer().getOrganization(),
                connection.getCardIssuer().getName(),
                connection.getStatus().name(),
                connection.getCreatedAt()
        );
    }
}
