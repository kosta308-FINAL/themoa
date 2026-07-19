package com.weaone.themoa.domain.policy.rag.dto;

/**
 * 정책 대상·자격이 일반 청년에게 얼마나 넓게 열려 있는지 나타내는 범용성 분류다.
 *
 * <p>지역·나이·취업·교육 단계처럼 명확한 조건은 기존 Hard Filter가 처리한다. 이 값은 농식품 바우처,
 * 예술인 전용 적립계좌처럼 경제 키워드는 강하지만 대상이 특수한 정책을 일반 경제 검색에서 낮추기 위한
 * ranking 신호이며, 사용자가 해당 조건을 명시하면 감점을 줄인다.</p>
 */
public enum EligibilityBreadth {
    BROAD,
    MODERATE,
    RESTRICTED,
    HIGHLY_RESTRICTED,
    UNKNOWN
}
