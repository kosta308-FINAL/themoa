package com.weaone.themoa.domain.cardtransaction.client;

import java.time.LocalDate;

/**
 * CODEF 카드 승인내역 조회 요청 입력. {@code inquiryType}은 항상 "1"(전체 통합조회, cardtransaction.md §6-2
 * 확정값)이라 커맨드에 노출하지 않고 클라이언트가 고정한다 — 카드별 선택 조회는 이 기능 스코프에 없다.
 */
public record CodefApprovalListCommand(
        String organization,
        String connectedId,
        String birthDate,
        LocalDate startDate,
        LocalDate endDate
) {
}
