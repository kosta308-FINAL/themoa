package com.weaone.themoa.domain.budget.service;

/**
 * 월급·저축 목표 변경 적용 시점(dailyBudget.md §1, MOA-S-BUD-BGT-08). 어느 쪽이든 {@code member} 원본은
 * 즉시 갱신되고, 선택은 현재 주기 {@code budget} 스냅샷을 지금 덮어쓸지 여부만 가른다.
 */
public enum BudgetApplyScope {
    /** 이번 급여 주기부터 — 현재 주기 스냅샷을 갱신해 월 예산·하루 권장액이 즉시 바뀐다. */
    CURRENT_CYCLE,
    /** 다음 급여 주기부터 — 현재 주기 스냅샷은 그대로 두고 다음 주기 생성 때 반영한다. */
    NEXT_CYCLE
}
