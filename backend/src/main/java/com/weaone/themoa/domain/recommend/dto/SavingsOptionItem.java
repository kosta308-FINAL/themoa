package com.weaone.themoa.domain.recommend.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 예금/적금 공통 optionList 원소 (금리 옵션).
 * baseList와는 fin_co_no + fin_prdt_cd 조합으로 매칭한다(한 상품에 기간별 여러 옵션).
 * rsrv_type(적립방식)은 적금에만 있고 예금은 null로 온다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SavingsOptionItem(
        @JsonProperty("fin_co_no") String finCoNo,
        @JsonProperty("fin_prdt_cd") String finPrdtCd,
        @JsonProperty("intr_rate_type") String intrRateType,       // S:단리 M:복리
        @JsonProperty("intr_rate_type_nm") String intrRateTypeNm,  // 단리/복리
        @JsonProperty("rsrv_type") String rsrvType,                // S:정액 F:자유 (적금만)
        @JsonProperty("rsrv_type_nm") String rsrvTypeNm,           // 정액적립식/자유적립식 (적금만)
        @JsonProperty("save_trm") String saveTrm,                  // 저축기간(개월), 문자열로 옴
        @JsonProperty("intr_rate") BigDecimal intrRate,            // 기본 금리
        @JsonProperty("intr_rate2") BigDecimal intrRate2           // 최고 우대금리
) {
}
