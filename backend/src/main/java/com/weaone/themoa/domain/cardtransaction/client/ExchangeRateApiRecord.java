package com.weaone.themoa.domain.cardtransaction.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 한국수출입은행 환율 API(exchangeJSON) 응답 1행. {@code result=1}만 성공이다.
 * {@code dealBasR}은 {@code "1,392.40"}처럼 콤마가 섞인 문자열로 온다(cardtransaction.md §4).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateApiRecord(
        @JsonProperty("result") int result,
        @JsonProperty("cur_unit") String curUnit,
        @JsonProperty("deal_bas_r") String dealBasR
) {
}
