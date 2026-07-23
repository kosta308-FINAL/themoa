package com.weaone.themoa.domain.recommend.dto.response;

import com.weaone.themoa.domain.recommend.ingest.CollectionBatchService;
import com.weaone.themoa.domain.recommend.ingest.LoanIngestService;
import com.weaone.themoa.domain.recommend.ingest.SavingsIngestService;

import java.time.LocalDateTime;

/**
 * 관리자 수동 수집 결과. 예적금·대출 파트가 각각 독립적으로 재시도되므로 한쪽만 실패할 수 있다.
 *
 * <p>수집만으로는 검색 인덱스에 반영되지 않는다. 신규 상품을 검색에 태우려면 수집 후
 * 임베딩 재구성(`POST /api/financial-products/embeddings/rebuild`)을 이어서 실행해야 한다.
 *
 * @param completedAt 수집이 끝난 시각(Asia/Seoul). 화면에서 "몇시 몇분에 완료" 안내에 쓴다.
 */
public record FinancialCollectResponse(
        Part savings,
        Part loans,
        LocalDateTime completedAt
) {

    /**
     * 파트별 집계.
     *
     * @param success       최종 성공 여부(3회 재시도 후에도 실패하면 false, 나머지 값은 0)
     * @param fetched       finlife에서 받은 상품 수
     * @param skippedClosed 판매종료로 제외한 수
     * @param inserted      신규 저장 수
     * @param updated       기존 갱신 수
     */
    public record Part(
            boolean success,
            int fetched,
            int skippedClosed,
            int inserted,
            int updated
    ) {
        private static final Part FAILED = new Part(false, 0, 0, 0, 0);
    }

    public static FinancialCollectResponse from(CollectionBatchService.CollectionResult result,
                                                LocalDateTime completedAt) {
        return new FinancialCollectResponse(fromSavings(result.savings()), fromLoans(result.loans()), completedAt);
    }

    private static Part fromSavings(SavingsIngestService.IngestSummary summary) {
        if (summary == null) {
            return Part.FAILED;
        }
        return new Part(true, summary.fetched, summary.skippedClosed, summary.inserted, summary.updated);
    }

    private static Part fromLoans(LoanIngestService.IngestSummary summary) {
        if (summary == null) {
            return Part.FAILED;
        }
        return new Part(true, summary.fetched, summary.skippedClosed, summary.inserted, summary.updated);
    }
}
