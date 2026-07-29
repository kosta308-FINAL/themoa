package com.weaone.themoa.domain.member.dto;

import java.time.LocalDateTime;

/** 회원 관리 화면에 보여줄 계정 상태. DB 컬럼이 아니라 lockedUntil/withdrawnAt으로부터 매 조회 시 계산한다. */
public enum AdminMemberStatus {
    ACTIVE,
    LOCKED,
    WITHDRAWN;

    public static AdminMemberStatus of(LocalDateTime lockedUntil, LocalDateTime withdrawnAt, LocalDateTime now) {
        if (withdrawnAt != null) {
            return WITHDRAWN;
        }
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return LOCKED;
        }
        return ACTIVE;
    }
}
