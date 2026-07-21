package com.weaone.themoa.domain.financialsearch.dto;

import java.math.BigDecimal;

// 예금/적금/대출을 한 화면에서 같이 보여주기 위한 공통 결과 형태.
// representativeRate: 예금/적금은 옵션 중 최고금리(maxRate), 대출은 최저금리(rateMin) — 상품 성격이 달라서 기준이 다름.
// representativeTermMonth: 대출엔 가입기간 개념이 없어서 null.
// matchReason: 이 상품이 왜 검색 결과에 나왔는지 설명. OpenAI가 켜져있으면 LLM이 상품 조건을 보고 직접
// 작성하고, 꺼져있거나 실패하면 규칙 기반(키워드 일치/의미 유사도 신호) 설명으로 자동 대체된다.
// officialUrl: 은행 공식 홈페이지 링크. 매핑 안 된 곳은 은행명 검색 링크로 대체(정확한 공식 URL 보장 안 됨).
public record FinancialSearchResultItem(
        Long id,
        String productType,
        String companyName,
        String productName,
        String joinMethod,
        BigDecimal representativeRate,
        Integer representativeTermMonth,
        String specialCondition,
        String matchReason,
        String officialUrl
) {
    public FinancialSearchResultItem withMatchReason(String newMatchReason) {
        return new FinancialSearchResultItem(id, productType, companyName, productName, joinMethod,
                representativeRate, representativeTermMonth, specialCondition, newMatchReason, officialUrl);
    }
}
