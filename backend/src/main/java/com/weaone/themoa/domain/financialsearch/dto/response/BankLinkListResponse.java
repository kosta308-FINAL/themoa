package com.weaone.themoa.domain.financialsearch.dto.response;

import java.util.List;

/**
 * 은행 공식 링크 관리 화면 데이터.
 *
 * @param links                  등록된 링크(회사명 순)
 * @param companiesWithoutLink   판매중인 상품에는 있는데 검증된 링크가 없는 금융회사.
 *                               이 회사들의 상품은 결과 화면에서 공식 홈페이지 대신 검색 링크로 나간다.
 */
public record BankLinkListResponse(
        List<BankLinkResponse> links,
        List<String> companiesWithoutLink
) {

    public record BankLinkResponse(String companyName, String officialUrl) {
    }
}
