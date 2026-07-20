package com.weaone.themoa.domain.cardconnection.dto.response;

import java.util.List;

/** S-01 "카드 관리" 팝업(entryMode.md §2-1): 연결된 카드사 목록 + 자동수집 ON/OFF 상태. */
public record CardConnectionListResponse(
        boolean cardSyncEnabled,
        List<CardConnectionResponse> connections
) {
}
