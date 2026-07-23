package com.weaone.themoa.domain.financialsearch.entity;

/**
 * 검색어 해석에 쓰는 키워드의 종류.
 *
 * <p>둘 다 "그룹 → 키워드 목록" 구조가 같아 한 테이블로 관리한다.
 */
public enum SearchKeywordType {

    /**
     * 인구집단 키워드. 그룹키는 YOUTH·SENIOR 등이고, 검색어와 상품이 서로 다른 집단에만 해당하면
     * 결과에서 제외하는 하드필터로 쓴다. 같은 그룹 안의 단어는 서로 동의어로 확장된다
     * (예: "임산부"로 검색해도 상품 텍스트의 "임신"이 걸리도록).
     */
    DEMOGRAPHIC,

    /**
     * 상품유형 의도 키워드. 그룹키는 SAVINGS·LOAN이고, 검색어에 이 단어가 있으면 해당 유형만 후보로 남긴다.
     * 저축 신호가 대출 신호보다 우선한다.
     */
    PRODUCT_INTENT
}
