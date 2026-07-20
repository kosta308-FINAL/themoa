package com.weaone.themoa.domain.coaching.dto.response;

import java.util.List;

/**
 * S-01 습관 코칭 카드 목록(dayguide.md §3.5·§8.1). 정상적으로 비어 있는 상태(카드 미연동, 데이터 부족)는
 * 오류가 아니라 {@code items=[]}와 {@code emptyReason}으로 구분한다.
 */
public record CoachingCardListResponse(
        List<CoachingCardResponse> items,
        String emptyReason
) {
}
