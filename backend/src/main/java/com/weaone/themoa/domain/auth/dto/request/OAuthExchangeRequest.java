package com.weaone.themoa.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthExchangeRequest(
        @NotBlank(message = "교환코드가 필요합니다.")
        String code
) {
}
