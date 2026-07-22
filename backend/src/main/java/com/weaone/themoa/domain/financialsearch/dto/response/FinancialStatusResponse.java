package com.weaone.themoa.domain.financialsearch.dto.response;

import java.time.LocalDateTime;

/**
 * 금융상품 데이터 현황(관리자 대시보드).
 *
 * @param savings              예·적금 상품 수
 * @param loans                대출 상품 수
 * @param lastCollectedAt      마지막으로 상품이 갱신된 시각. 수집 배치가 upsert하며 갱신하므로 사실상
 *                             "마지막 수집 시각"이다. 상품이 하나도 없으면 null
 * @param vectorSearchEnabled  벡터검색 사용 여부(app.financial.rag.enabled). false면 키워드 검색만 동작한다
 * @param indexedDocumentCount 벡터 인덱스에 들어있는 문서 수. 상품 수와 다르면 인덱스 갱신이 필요하다는 신호다.
 *                             벡터검색이 꺼져 있거나 Qdrant 조회에 실패하면 null
 * @param bankLinks            은행 공식 링크 등록 현황
 */
public record FinancialStatusResponse(
        ProductCount savings,
        ProductCount loans,
        LocalDateTime lastCollectedAt,
        boolean vectorSearchEnabled,
        Long indexedDocumentCount,
        BankLinkStatus bankLinks
) {

    /**
     * @param total   전체 상품 수(판매종료 포함)
     * @param selling 판매중인 상품 수. 검색·추천 대상은 이 쪽이다
     */
    public record ProductCount(long total, long selling) {
    }

    /**
     * @param registered 공식 링크가 등록된 금융회사 수
     * @param missing    링크가 없어 검색 링크로 대체되고 있는 금융회사 수
     */
    public record BankLinkStatus(int registered, int missing) {
    }
}
