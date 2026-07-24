package com.weaone.themoa.domain.financialsearch.dto.response;

import java.util.List;

/**
 * 검색 키워드 관리 화면 데이터.
 *
 * @param demographicGroups 인구집단 그룹(YOUTH, SENIOR 등)별 키워드
 * @param productIntents    상품의도 그룹(SAVINGS, LOAN)별 키워드
 */
public record FinancialSearchKeywordListResponse(
        List<KeywordGroup> demographicGroups,
        List<KeywordGroup> productIntents
) {

    /**
     * @param groupKey 그룹 이름
     * @param keywords 그룹에 속한 키워드(삭제할 때 id가 필요해 함께 내려준다)
     */
    public record KeywordGroup(
            String groupKey,
            List<KeywordItem> keywords
    ) {
    }

    public record KeywordItem(Long id, String keyword) {
    }
}
