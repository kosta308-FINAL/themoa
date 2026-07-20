package com.weaone.themoa.domain.member.entity;

/**
 * 소득유형(dailyBudget.md, 알바 소득 확장). SALARY는 고정 월급, HOURLY는 요일별 근무시간×시급으로
 * 급여주기마다 예상 소득이 달라진다. 최초 소비가이드 설정 이후 서로 전환하는 일반 UI는 제공하지
 * 않는다(payday와 동일한 제약).
 */
public enum IncomeType {
    SALARY,
    HOURLY
}
