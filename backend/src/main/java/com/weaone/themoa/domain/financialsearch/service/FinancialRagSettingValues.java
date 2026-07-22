package com.weaone.themoa.domain.financialsearch.service;

/**
 * 검색에 실제로 적용되는 튜닝값. DB 설정이 있으면 그 값, 없으면 application.yaml 기본값이 담긴다.
 * 검색 한 번에 한 번만 읽어서 이 객체로 넘긴다(중간에 값이 바뀌어도 한 요청 안에서는 일관되도록).
 */
public record FinancialRagSettingValues(
        int topK,
        int retryTopK,
        double minimumSimilarity
) {
}
