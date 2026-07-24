package com.weaone.themoa.domain.fixedexpense.service;

import java.math.BigDecimal;

/**
 * 고정지출 코칭 카드 후보 1건. 규칙 계층(DONATION 카테고리·dismiss 제외)을 통과한 활성 고정지출이며,
 * 실제로 카드에 올릴지(월세·관리비·보험처럼 필수 성격인지)는 LLM이 name·categoryName을 보고 고른다.
 */
public record FixedExpenseCoachingCandidate(
        Long fixedExpenseId,
        String name,
        String categoryName,
        BigDecimal monthlyAmount,
        BigDecimal annualAmount
) {

    /** LLM이 그대로 되돌려주는 식별자 — 이름 문자열을 지어내지 못하게 한다. */
    public String targetRef() {
        return String.valueOf(fixedExpenseId);
    }
}
