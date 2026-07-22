package com.weaone.themoa.domain.financialsearch.dto.response;

import java.time.LocalDateTime;

/**
 * 검색 튜닝값 현재 상태.
 *
 * @param topK                최종 결과 개수
 * @param retryTopK           벡터검색 후보 개수
 * @param minimumSimilarity   유사도 임계값
 * @param usingDefaults       아직 한 번도 바꾸지 않아 application.yaml 기본값을 쓰는 중이면 true
 * @param updatedAt           마지막 변경 시각. 기본값을 쓰는 중이면 null
 * @param vectorSearchEnabled 벡터검색 사용 여부. 이 값은 화면에서 바꿀 수 없다(벡터스토어가 기동 시점에
 *                            만들어져서 런타임 변경이 실제로 반영되지 않는다) — 설정·재기동으로 다룬다
 */
public record FinancialRagSettingResponse(
        int topK,
        int retryTopK,
        double minimumSimilarity,
        boolean usingDefaults,
        LocalDateTime updatedAt,
        boolean vectorSearchEnabled
) {
}
