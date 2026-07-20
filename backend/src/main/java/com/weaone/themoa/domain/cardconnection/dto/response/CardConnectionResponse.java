package com.weaone.themoa.domain.cardconnection.dto.response;

import com.weaone.themoa.domain.cardconnection.entity.CardConnection;

import java.time.LocalDateTime;

public record CardConnectionResponse(
        Long connectionId,
        String organization,
        String organizationName,
        String connectionStatus,
        String entryMode,
        String initialSyncStatus,
        LocalDateTime lastSuccessfulSyncAt,
        LocalDateTime connectedAt
) {

    public static CardConnectionResponse from(CardConnection connection) {
        return new CardConnectionResponse(
                connection.getId(),
                connection.getCardIssuer().getOrganization(),
                connection.getCardIssuer().getName(),
                connection.getStatus().name(),
                connection.getMember().getEntryMode().name(),
                connection.getInitialSyncStatus().name(),
                connection.getLastSuccessfulSyncAt(),
                connection.getCreatedAt()
        );
    }
}
