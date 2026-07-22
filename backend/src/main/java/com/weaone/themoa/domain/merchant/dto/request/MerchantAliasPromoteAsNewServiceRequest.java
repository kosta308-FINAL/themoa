package com.weaone.themoa.domain.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MerchantAliasPromoteAsNewServiceRequest(
        @NotBlank String aliasText,
        @NotBlank String canonicalServiceName,
        Long categoryId
) {
}
