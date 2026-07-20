package com.weaone.themoa.domain.merchant.dto.response;

import java.math.BigDecimal;

/** 미식별 & '기타' 가맹점 작업대 한 행(merchant.md §2-1 전역 시드 후보). */
public record AdminUnclassifiedMerchantResponse(
        Long merchantId,
        String merchantNameRaw,
        String merchantTypeRaw,
        long transactionCount,
        BigDecimal averageAmount
) {
}
