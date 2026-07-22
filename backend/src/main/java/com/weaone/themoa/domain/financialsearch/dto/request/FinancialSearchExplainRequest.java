package com.weaone.themoa.domain.financialsearch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 검색 품질 점검 요청(관리자). 정렬 옵션은 채점과 무관해서 받지 않는다. */
public record FinancialSearchExplainRequest(

        @NotBlank(message = "점검할 검색어를 입력해 주세요.")
        @Size(min = 2, max = 200)
        String query
) {
}
