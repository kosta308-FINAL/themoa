package com.weaone.themoa.domain.subscription.dto.response;

/**
 * 관리자 우대조건 관리 화면의 상품 목록 한 줄.
 *
 * <p>상품 기본정보에 그 상품의 캐시 상태(항목 수·잠금·재검토·캐시 존재 여부)를 함께 담아,
 * 관리자가 어떤 상품을 손봐야 하는지 목록에서 바로 판단하고 productId를 얻게 한다.
 */
public record ProductConditionSummaryResponse(
        Long productId,
        String companyName,
        String productName,
        String productType,
        int itemCount,
        boolean editedByAdmin,
        boolean stale,
        boolean cached) {
}
