package com.weaone.themoa.domain.recommend.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 주택담보대출 / 전세자금대출 공통 optionList 원소 (금리).
 * 담보유형(mrtg_type)은 주택담보대출에만 있고 전세자금대출은 null로 온다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanRateOptionItem(
        @JsonProperty("fin_co_no") String finCoNo,
        @JsonProperty("fin_prdt_cd") String finPrdtCd,
        @JsonProperty("mrtg_type") String mrtgType,               // A:아파트 등 (주담대만)
        @JsonProperty("mrtg_type_nm") String mrtgTypeNm,          // (주담대만)
        @JsonProperty("rpay_type") String rpayType,               // 상환방식코드
        @JsonProperty("rpay_type_nm") String rpayTypeNm,          // 상환방식명
        @JsonProperty("lend_rate_type") String lendRateType,      // 금리유형코드
        @JsonProperty("lend_rate_type_nm") String lendRateTypeNm, // 금리유형명
        @JsonProperty("lend_rate_min") BigDecimal lendRateMin,    // 최저금리
        @JsonProperty("lend_rate_max") BigDecimal lendRateMax,    // 최고금리
        @JsonProperty("lend_rate_avg") BigDecimal lendRateAvg     // 평균금리
) {
}
