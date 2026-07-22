package com.weaone.themoa.domain.financialsearch.service;

import java.util.List;
import java.util.Map;

/**
 * 검색어 해석 키워드의 기본값. 테이블이 비어 있을 때 이 값으로 채우고, "기본값으로 초기화"도 이 값을 쓴다.
 *
 * <p>운영 중 관리자가 수정한 내용은 DB에만 반영되며 여기 값은 바뀌지 않는다.
 */
public final class FinancialSearchKeywordDefaults {

    /** 상품의도 그룹키. 저축 신호가 대출 신호보다 우선한다. */
    public static final String GROUP_SAVINGS = "SAVINGS";
    public static final String GROUP_LOAN = "LOAN";

    /**
     * 인구집단별 키워드. 검색어가 어느 집단에 해당하는지 감지하고, 같은 그룹 안의 단어끼리 동의어로 확장한다.
     * (예: 상품 텍스트엔 "임신"만 있고 "임산부"는 없는 경우가 많아 확장이 필요하다)
     */
    public static final Map<String, List<String>> DEMOGRAPHIC_GROUPS = Map.ofEntries(
            Map.entry("YOUTH", List.of("청년", "MZ", "사회초년생", "청소년", "대학생")),
            Map.entry("CHILDCARE", List.of("임산부", "임신", "출산", "육아", "다자녀", "아이사랑", "자녀", "키즈", "꿈나무", "아동")),
            Map.entry("DISABILITY", List.of("장애인", "장애우", "장애")),
            Map.entry("SENIOR", List.of("시니어", "고령자", "실버", "어르신", "노인")),
            Map.entry("MULTICULTURAL", List.of("다문화", "외국인")),
            Map.entry("VETERAN", List.of("국가유공자", "보훈", "상이군경")),
            Map.entry("LOW_INCOME", List.of("기초생활수급자", "차상위", "서민", "저소득")),
            Map.entry("FARMER_FISHER", List.of("농업인", "어업인", "농어민", "귀농", "귀어")),
            Map.entry("NEWLYWED", List.of("신혼부부", "예비부부")));

    /** 대출을 원한다고 볼 표현. */
    public static final List<String> LOAN_INTENT_KEYWORDS =
            List.of("대출", "빌리", "빌려", "급전", "돈이 없", "돈 없");

    /** 예금·적금(저축)을 원한다고 볼 표현. */
    public static final List<String> SAVINGS_INTENT_KEYWORDS =
            List.of("적금", "예금", "저축", "모으", "불리");

    private FinancialSearchKeywordDefaults() {
    }
}
