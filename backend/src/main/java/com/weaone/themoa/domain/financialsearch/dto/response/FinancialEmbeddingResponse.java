package com.weaone.themoa.domain.financialsearch.dto.response;

import java.time.LocalDateTime;

/**
 * 임베딩 재구성 결과.
 *
 * @param embeddedCount 벡터 인덱스에 넣은 문서 수(신규분만이 아니라 전체를 다시 임베딩한다)
 * @param completedAt   재구성이 끝난 시각(Asia/Seoul). 화면에서 "몇시 몇분에 완료" 안내에 쓴다.
 */
public record FinancialEmbeddingResponse(
        int embeddedCount,
        LocalDateTime completedAt
) {
}
