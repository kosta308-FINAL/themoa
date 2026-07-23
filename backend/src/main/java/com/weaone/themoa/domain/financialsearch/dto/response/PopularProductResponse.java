package com.weaone.themoa.domain.financialsearch.dto.response;

import java.math.BigDecimal;

/**
 * 실시간 인기 금융상품 1건(북마크 많은 순).
 *
 * @param bookmarkCount 이 상품을 북마크한 사람 수(순위 기준)
 * @param rate          대표금리(예·적금은 최고금리)
 * @param termMonth     대표 가입기간(개월)
 */
public record PopularProductResponse(
        int rank,
        Long productId,
        String productName,
        String companyName,
        String productType,
        BigDecimal rate,
        Integer termMonth,
        long bookmarkCount,
        String officialUrl
) {
}
