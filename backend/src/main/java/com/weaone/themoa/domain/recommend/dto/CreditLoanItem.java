package com.weaone.themoa.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 개인신용대출 baseList 원소.
 * 주담대/전세와 달리 CB사(cb_name)·신용대출유형(crdt_prdt_type) 필드를 갖고,
 * 대출한도/부대비용/연체이자 필드는 없다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditLoanItem(
        @JsonProperty("fin_co_no") String finCoNo,
        @JsonProperty("fin_prdt_cd") String finPrdtCd,
        @JsonProperty("crdt_prdt_type") String crdtPrdtType,       // 신용대출 상품유형코드
        @JsonProperty("crdt_prdt_type_nm") String crdtPrdtTypeNm,  // 신용대출 상품유형명
        @JsonProperty("kor_co_nm") String korCoNm,
        @JsonProperty("fin_prdt_nm") String finPrdtNm,
        @JsonProperty("join_way") String joinWay,
        @JsonProperty("cb_name") String cbName,                    // 신용평가회사명
        @JsonProperty("dcls_strt_day") String dclsStrtDay,
        @JsonProperty("dcls_end_day") String dclsEndDay
) {
}
