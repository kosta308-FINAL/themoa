package com.weaone.themoa.domain.cardconnection.dto.response;

import java.util.List;

/** 초기수집 상태 폴링(dayguide.md §8.1·§8.5). 폴링은 완료·실패 시 클라이언트가 중단한다. */
public record InitialSyncStatusResponse(
        String overallStatus,
        List<ConnectionSyncStatus> connections
) {

    public record ConnectionSyncStatus(
            Long connectionId,
            String organization,
            String organizationName,
            String initialSyncStatus,
            String initialSyncErrorCode
    ) {
    }
}
