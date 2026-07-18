package com.weaone.themoa.domain.cardconnection.dto.request;

import jakarta.validation.constraints.NotNull;

/** 카드 자동수집 ON/OFF(entryMode.md §2-1). {@code false}는 entry_mode 역전이가 아니라 이 플래그로만 표현된다. */
public record CardSyncSettingRequest(
        @NotNull Boolean enabled
) {
}
