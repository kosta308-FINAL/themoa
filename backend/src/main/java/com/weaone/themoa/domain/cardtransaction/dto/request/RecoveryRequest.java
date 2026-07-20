package com.weaone.themoa.domain.cardtransaction.dto.request;

import com.weaone.themoa.domain.cardtransaction.service.RecoveryMode;
import jakarta.validation.constraints.NotNull;

/** 클라이언트는 시작일을 직접 계산하지 않고 이 두 모드만 보낸다(cardtransaction.md §6-C). */
public record RecoveryRequest(
        @NotNull RecoveryMode mode
) {
}
