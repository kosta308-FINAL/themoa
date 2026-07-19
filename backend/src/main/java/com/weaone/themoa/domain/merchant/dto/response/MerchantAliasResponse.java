package com.weaone.themoa.domain.merchant.dto.response;

import com.weaone.themoa.domain.merchant.entity.MerchantAlias;

/** F-03 가맹점 검색/선택 드롭다운(view/fixedExpense.md §3.3)에 쓰이는 전역 alias 카탈로그 항목. */
public record MerchantAliasResponse(
        Long id,
        String canonicalServiceName,
        Long defaultCategoryId,
        String defaultCategoryName
) {

    public static MerchantAliasResponse from(MerchantAlias merchantAlias) {
        return new MerchantAliasResponse(
                merchantAlias.getId(),
                merchantAlias.getCanonicalServiceName(),
                merchantAlias.getDefaultCategory() == null ? null : merchantAlias.getDefaultCategory().getId(),
                merchantAlias.getDefaultCategory() == null ? null : merchantAlias.getDefaultCategory().getName()
        );
    }
}
