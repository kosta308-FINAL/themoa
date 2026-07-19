package com.weaone.themoa.domain.policy.rag.dto;

/**
 * 사용자 검색에서 세부 SupportIntent를 넓게 묶어 쓰는 혜택 의도다.
 *
 * <p>지원금, 수당, 대출, 저축, 주거비처럼 표현은 다르지만 사용자가 기대하는 결과가
 * "경제적 부담 완화"인 경우가 많다. 세부 SupportIntent는 관리자 진단과 정책 태그로 보존하고,
 * 사용자 검색의 후보 확장과 ranking에는 이 넓은 그룹을 함께 사용해 검색 범위를 과도하게 좁히지 않는다.</p>
 */
public enum BenefitGroup {
    ECONOMIC_SUPPORT,
    HOUSING_SUPPORT,
    EMPLOYMENT_SUPPORT,
    EDUCATION_SUPPORT,
    GENERAL_BENEFIT
}
