package com.weaone.themoa.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 주택담보대출 / 전세자금대출 공통 baseList 원소.
 * 두 API(mortgageLoanProductsSearch, rentHouseLoanProductsSearch)의 base 필드가 동일하다.
 * 예·적금과 달리 우대조건(spcl_cnd)/만기후이자(mtrt_int)/최고한도(max_limit)가 없고, 대출 전용 필드가 있다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanBaseItem(
        @JsonProperty("fin_co_no") String finCoNo,
        @JsonProperty("fin_prdt_cd") String finPrdtCd,
        @JsonProperty("kor_co_nm") String korCoNm,
        @JsonProperty("fin_prdt_nm") String finPrdtNm,
        @JsonProperty("join_way") String joinWay,
        @JsonProperty("loan_inci_expn") String loanInciExpn,   // 대출 부대비용
        @JsonProperty("erly_rpay_fee") String erlyRpayFee,     // 중도상환수수료
        @JsonProperty("dly_rate") String dlyRate,              // 연체이자율
        @JsonProperty("loan_lmt") String loanLmt,              // 대출한도(텍스트)
        @JsonProperty("dcls_strt_day") String dclsStrtDay,
        @JsonProperty("dcls_end_day") String dclsEndDay
) {
}
