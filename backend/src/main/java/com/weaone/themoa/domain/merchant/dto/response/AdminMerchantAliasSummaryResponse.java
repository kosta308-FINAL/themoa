package com.weaone.themoa.domain.merchant.dto.response;

/** 관리자 "서비스(MerchantAlias) 전체 목록" 한 행 — 중복 생성된 서비스를 눈으로 찾아 병합하기 위한 자료다. */
public record AdminMerchantAliasSummaryResponse(
        Long aliasId,
        String canonicalServiceName,
        String categoryName,
        long fixedExpenseCount,
        long merchantCount
) {
}
