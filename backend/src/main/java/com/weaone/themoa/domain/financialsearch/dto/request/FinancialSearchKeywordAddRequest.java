package com.weaone.themoa.domain.financialsearch.dto.request;

import com.weaone.themoa.domain.financialsearch.entity.SearchKeywordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 검색 키워드 추가 요청.
 *
 * @param keywordType DEMOGRAPHIC(인구집단) 또는 PRODUCT_INTENT(상품의도)
 * @param groupKey    인구집단이면 YOUTH·SENIOR 등, 상품의도면 SAVINGS 또는 LOAN
 * @param keyword     추가할 단어
 */
public record FinancialSearchKeywordAddRequest(

        @NotNull(message = "키워드 종류를 선택해 주세요.")
        SearchKeywordType keywordType,

        @NotBlank(message = "그룹을 입력해 주세요.")
        @Size(max = 30, message = "그룹은 30자 이하여야 합니다.")
        String groupKey,

        @NotBlank(message = "키워드를 입력해 주세요.")
        @Size(max = 50, message = "키워드는 50자 이하여야 합니다.")
        String keyword
) {
}
