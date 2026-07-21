package com.weaone.themoa.domain.merchant.dto.response;

/** 전역 마스터 승격 대기목록 한 행(merchant.md §1 확장). */
public record AdminMerchantPromotionCandidateResponse(
        Long aliasId,
        String aliasText,
        String canonicalServiceName,
        String categoryName,
        long learnerCount
) {
}
