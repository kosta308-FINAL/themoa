package com.weaone.themoa.domain.subscription.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가입 등록 화면 초안. 상품을 고르면 이 값으로 폼을 채운다.
 *
 * <p>우대조건은 원문에서 정규식으로 쪼갠 "초안"이라 부정확할 수 있다. 화면에서 사용자가 항목·가산금리를
 * 확인·수정한 뒤 확정한다.
 *
 * @param baseRate   기본금리(연 %). 사용자가 우대조건을 체크하면 여기에 가산폭을 더해 적용금리를 만든다
 * @param maxRate    최고우대금리(연 %). 적용금리가 이 값을 넘으면 화면에서 경고한다
 * @param joinMethod 가입방법(인터넷·영업점 등)
 * @param officialUrl 금융회사 공식 홈페이지(없으면 검색 링크)
 * @param termMonths 상품이 제공하는 가입기간(개월) 목록
 * @param conditions 우대조건 초안 항목
 */
public record SubscriptionDraftResponse(
        Long productId,
        String productName,
        String companyName,
        String productType,
        BigDecimal baseRate,
        BigDecimal maxRate,
        String joinMethod,
        String officialUrl,
        List<Integer> termMonths,
        List<ConditionDraft> conditions
) {

    public record ConditionDraft(String description, BigDecimal rateBonus) {
    }
}
