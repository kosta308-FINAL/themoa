package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.coaching.entity.CoachingCardTargetType;

import java.math.BigDecimal;

/**
 * 규칙 계층이 산출한 습관 후보 1개(habitExpense.md §3). LLM 프롬프트 입력과 폴백 템플릿 조립 양쪽의
 * 공통 재료다. {@code categoryId}는 두 유형 모두에서 채워진다(가맹점 별칭 후보의 절감 비율도 카테고리
 * 기준으로 정해지므로 §3).
 */
public record HabitCoachingCandidate(
        CoachingCardTargetType targetType,
        Long categoryId,
        Long merchantAliasId,
        String label,
        long transactionCount,
        BigDecimal totalNetAmount,
        BigDecimal avgPerTransaction,
        BigDecimal monthlyAverage,
        BigDecimal estimatedSaving,
        boolean toneDown
) {

    /** LLM이 그대로 되돌려주는 식별자 — 이름 문자열을 지어내지 못하게 한다(§4). */
    public String targetRef() {
        return targetType.name() + ":" + (targetType == CoachingCardTargetType.CATEGORY ? categoryId : merchantAliasId);
    }
}
