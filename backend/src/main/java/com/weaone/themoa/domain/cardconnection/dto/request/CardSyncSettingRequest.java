package com.weaone.themoa.domain.cardconnection.dto.request;

import com.weaone.themoa.domain.cardtransaction.service.RecoveryMode;
import jakarta.validation.constraints.NotNull;

/**
 * 카드 자동수집 ON/OFF(entryMode.md §2-1, dayguide.md §8.1). {@code false}는 entry_mode 역전이가 아니라
 * 이 플래그로만 표현된다. {@code recoveryMode}는 OFF→ON 갭 백필(§4-1)의 시작점 정책이며 미지정 시
 * {@link RecoveryMode#RECOVER_RECENT}(기존 자동 판단)로 동작한다.
 */
public record CardSyncSettingRequest(
        @NotNull Boolean enabled,
        RecoveryMode recoveryMode
) {
}
