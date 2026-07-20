package com.weaone.themoa.domain.recommend.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 개인신용대출 optionList 원소 (신용점수 구간별 금리).
 * 주담대/전세의 min/max/avg와 달리, 신용점수 구간별(crdt_grad_*) 금리를 제공한다.
 * crdt_grad_1(900점초과) ~ crdt_grad_13(하위) + crdt_grad_avg(평균).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditLoanOptionItem(
        @JsonProperty("fin_co_no") String finCoNo,
        @JsonProperty("fin_prdt_cd") String finPrdtCd,
        @JsonProperty("crdt_prdt_type") String crdtPrdtType,
        @JsonProperty("crdt_lend_rate_type") String crdtLendRateType,      // 금리구분코드
        @JsonProperty("crdt_lend_rate_type_nm") String crdtLendRateTypeNm, // 금리구분명
        @JsonProperty("crdt_grad_1") BigDecimal crdtGrad1,     // 900점 초과
        @JsonProperty("crdt_grad_4") BigDecimal crdtGrad4,     // 801~900
        @JsonProperty("crdt_grad_5") BigDecimal crdtGrad5,     // 701~800
        @JsonProperty("crdt_grad_6") BigDecimal crdtGrad6,     // 601~700
        @JsonProperty("crdt_grad_10") BigDecimal crdtGrad10,   // 501~600
        @JsonProperty("crdt_grad_11") BigDecimal crdtGrad11,   // 401~500
        @JsonProperty("crdt_grad_12") BigDecimal crdtGrad12,   // 301~400
        @JsonProperty("crdt_grad_13") BigDecimal crdtGrad13,   // 300 이하
        @JsonProperty("crdt_grad_avg") BigDecimal crdtGradAvg  // 평균
) {
}
