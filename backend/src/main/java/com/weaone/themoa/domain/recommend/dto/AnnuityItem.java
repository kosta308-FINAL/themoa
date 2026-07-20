package com.weaone.themoa.domain.recommend.dto;

import com.weaone.themoa.domain.recommend.entity.PensionProduct;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 연금저축 baseList 원소 (상품 기본정보).
 * ⚠️ finlife 연금 API가 현재 total_count만 주고 목록을 빈 채로 반환하는 이슈가 있어 실측 미검증.
 *    finlife 문서/PensionProduct 엔티티 기준 필드명으로 정의했고, 미지 필드는 무시(ignoreUnknown)한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnnuityItem(
        @JsonProperty("fin_co_no") String finCoNo,
        @JsonProperty("fin_prdt_cd") String finPrdtCd,
        @JsonProperty("kor_co_nm") String korCoNm,
        @JsonProperty("fin_prdt_nm") String finPrdtNm,
        @JsonProperty("join_way") String joinWay,
        @JsonProperty("pnsn_kind") String pnsnKind,            // 연금종류코드
        @JsonProperty("pnsn_kind_nm") String pnsnKindNm,       // 연금종류명
        @JsonProperty("prdt_type") String prdtType,            // 상품유형코드
        @JsonProperty("prdt_type_nm") String prdtTypeNm,       // 상품유형명
        @JsonProperty("avg_prft_rate") BigDecimal avgPrftRate, // 평균 수익률
        @JsonProperty("btrm_prft_rate_1") BigDecimal btrmPrftRate1, // 직전 1년 수익률
        @JsonProperty("btrm_prft_rate_2") BigDecimal btrmPrftRate2, // 직전 2년
        @JsonProperty("btrm_prft_rate_3") BigDecimal btrmPrftRate3, // 직전 3년
        @JsonProperty("sale_co") String saleCo,                // 판매회사
        @JsonProperty("dcls_strt_day") String dclsStrtDay,
        @JsonProperty("dcls_end_day") String dclsEndDay
) {
}
