package com.weaone.themoa.domain.recommend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * finlife API 공통 응답 껍데기.
 * 실제 JSON:  { "result": { total_count, max_page_no, err_cd, baseList[], optionList[] } }
 *
 * 상품(B)과 옵션(O) 타입만 갈아끼우면 예금/적금/연금/대출 응답에 모두 재사용된다.
 *   예) FinlifeApiResponse<DepositItem, DepositOptionItem>
 *
 * @param <B> baseList 원소 타입 (상품 기본정보)
 * @param <O> optionList 원소 타입 (금리/옵션)
 */
@JsonIgnoreProperties(ignoreUnknown = true)   // 안 쓰는 필드(dcls_month 등)는 무시
public record FinlifeApiResponse<B, O>(Result<B, O> result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result<B, O>(
            @JsonProperty("err_cd") String errCd,       // "000" = 정상
            @JsonProperty("err_msg") String errMsg,
            @JsonProperty("total_count") Integer totalCount,
            @JsonProperty("max_page_no") Integer maxPageNo,  // 마지막 페이지 번호
            @JsonProperty("now_page_no") Integer nowPageNo,
            @JsonProperty("baseList") List<B> baseList,
            @JsonProperty("optionList") List<O> optionList
    ) {
        /** 정상 응답인지(err_cd == "000"). */
        public boolean isSuccess() {
            return "000".equals(errCd);
        }
    }
}
