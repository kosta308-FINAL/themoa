package com.weaone.themoa.domain.recommend.dto;

import com.weaone.themoa.domain.recommend.entity.PensionProductOption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 연금저축 optionList 원소 (수령/납입 옵션).
 * ⚠️ AnnuityItem과 동일하게 현재 실측 미검증(finlife 빈 응답 이슈). PensionProductOption 엔티티 기준 필드명.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnnuityOptionItem(
        @JsonProperty("fin_co_no") String finCoNo,
        @JsonProperty("fin_prdt_cd") String finPrdtCd,
        @JsonProperty("pnsn_recp_trm") String pnsnRecpTrm,       // 수령기간코드
        @JsonProperty("pnsn_recp_trm_nm") String pnsnRecpTrmNm,  // 수령기간명
        @JsonProperty("pnsn_entr_age") Integer pnsnEntrAge,      // 가입나이
        @JsonProperty("mon_paym_atm") String monPaymAtm,         // 월납입금액코드
        @JsonProperty("paym_prd") Integer paymPrd,               // 납입기간(년)
        @JsonProperty("pnsn_strt_age") Integer pnsnStrtAge,      // 연금개시나이
        @JsonProperty("pnsn_recp_amt") Long pnsnRecpAmt          // 예상 연금수령액(원)
) {
}
