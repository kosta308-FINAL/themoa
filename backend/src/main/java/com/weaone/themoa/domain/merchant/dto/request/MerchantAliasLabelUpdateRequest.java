package com.weaone.themoa.domain.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회원 개인 표시명 설정. */
public record MerchantAliasLabelUpdateRequest(
        @NotBlank @Size(max = 255) String label
) {
}
