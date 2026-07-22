package com.weaone.themoa.domain.financialsearch.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 검색 튜닝값 변경 요청. null로 두면 그 항목은 application.yaml 기본값으로 되돌아간다.
 *
 * @param topK              최종 결과 개수(1~50)
 * @param retryTopK         벡터검색에서 받아올 후보 수(1~100)
 * @param minimumSimilarity 유사도 임계값(0~1). 낮출수록 의미가 느슨하게 걸리는 상품까지 결과에 들어온다
 */
public record FinancialRagSettingUpdateRequest(

        @Min(value = 1, message = "결과 개수는 1 이상이어야 합니다.")
        @Max(value = 50, message = "결과 개수는 50 이하여야 합니다.")
        Integer topK,

        @Min(value = 1, message = "후보 개수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "후보 개수는 100 이하여야 합니다.")
        Integer retryTopK,

        @DecimalMin(value = "0.0", message = "유사도 임계값은 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", message = "유사도 임계값은 1 이하여야 합니다.")
        Double minimumSimilarity
) {
}
