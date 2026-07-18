package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.category.entity.CategoryCode;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 습관 코칭 규칙 계층 상수(habitExpense.md §3). 절감 비율은 재량성 기준 2단이다 — 대체·생략이 쉬운 순수
 * 재량 소비는 0.5, 필수가 섞인 소비는 0.3(보수적). LLM이 아니라 규칙 계층이 계산한다.
 */
public final class HabitSavingRatioPolicy {

    private static final BigDecimal DISCRETIONARY = BigDecimal.valueOf(0.5);
    private static final BigDecimal MIXED_NECESSITY = BigDecimal.valueOf(0.3);

    /** 그 외/미분류 카테고리 기본값 — 모르면 보수적으로 잡는다. */
    private static final BigDecimal DEFAULT_RATIO = MIXED_NECESSITY;

    private static final Map<CategoryCode, BigDecimal> RATIOS = Map.of(
            CategoryCode.DELIVERY, DISCRETIONARY,
            CategoryCode.CAFE, DISCRETIONARY,
            CategoryCode.LEISURE, DISCRETIONARY,
            CategoryCode.CONVENIENCE, MIXED_NECESSITY,
            CategoryCode.FOOD, MIXED_NECESSITY,
            CategoryCode.TRANSPORT, MIXED_NECESSITY
    );

    /** 소비성 카테고리만 후보(줄일 여지가 있는 행동 소비). 의료·송금·쇼핑 등 필수·비행동·애매 항목은 제외. */
    public static final Set<CategoryCode> CONSUMPTION_CATEGORIES = Set.of(
            CategoryCode.FOOD, CategoryCode.DELIVERY, CategoryCode.CAFE,
            CategoryCode.CONVENIENCE, CategoryCode.TRANSPORT, CategoryCode.LEISURE
    );

    private HabitSavingRatioPolicy() {
    }

    public static BigDecimal ratioFor(CategoryCode code) {
        return RATIOS.getOrDefault(code, DEFAULT_RATIO);
    }
}
