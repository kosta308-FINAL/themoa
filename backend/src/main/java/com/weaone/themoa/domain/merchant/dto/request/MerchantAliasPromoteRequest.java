package com.weaone.themoa.domain.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MerchantAliasPromoteRequest(
        @NotNull Long aliasId,
        @NotBlank String aliasText
) {
}
