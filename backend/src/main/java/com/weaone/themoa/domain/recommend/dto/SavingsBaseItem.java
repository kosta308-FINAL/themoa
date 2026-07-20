package com.weaone.themoa.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 예금/적금 공통 baseList 원소 (상품 기본정보).
 * 예금(depositProductsSearch)과 적금(savingProductsSearch)의 base 필드는 완전히 동일해서 공유한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SavingsBaseItem(
        @JsonProperty("fin_co_no") String finCoNo,         // 금융회사 코드
        @JsonProperty("fin_prdt_cd") String finPrdtCd,     // 상품 코드 (중복체크 키)
        @JsonProperty("kor_co_nm") String korCoNm,         // 금융회사명
        @JsonProperty("fin_prdt_nm") String finPrdtNm,     // 상품명
        @JsonProperty("join_way") String joinWay,          // 가입방법
        @JsonProperty("mtrt_int") String mtrtInt,          // 만기 후 이자율
        @JsonProperty("spcl_cnd") String spclCnd,          // 우대조건
        @JsonProperty("join_deny") String joinDeny,        // 가입제한 1/2/3
        @JsonProperty("join_member") String joinMember,    // 가입대상(파싱 원문)
        @JsonProperty("etc_note") String etcNote,          // 기타 유의사항
        @JsonProperty("max_limit") Long maxLimit,          // 최고한도(원), null=한도없음
        @JsonProperty("dcls_strt_day") String dclsStrtDay, // 공시 시작일 YYYYMMDD
        @JsonProperty("dcls_end_day") String dclsEndDay    // 공시 종료일, null=판매중
) {
}
