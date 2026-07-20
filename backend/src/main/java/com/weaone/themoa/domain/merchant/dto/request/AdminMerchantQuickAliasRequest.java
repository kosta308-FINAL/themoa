package com.weaone.themoa.domain.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminMerchantQuickAliasRequest(
        @NotBlank String canonicalServiceName,
        Long categoryId
) {
}
