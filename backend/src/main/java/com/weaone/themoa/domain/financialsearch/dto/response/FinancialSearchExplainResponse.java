package com.weaone.themoa.domain.financialsearch.dto.response;

import java.util.List;

/**
 * 검색 품질 점검 결과(관리자 전용).
 *
 * <p>실제 검색과 같은 계산을 돌린 뒤, 결과만 주는 게 아니라 "왜 이 상품이 나왔는지 / 왜 빠졌는지"를
 * 점수와 함께 보여준다. 검색어가 기대와 다르게 동작할 때(예: 결과 0건) 원인이 유사도 임계값인지,
 * 인구집단 필터인지, 상품유형 의도 감지인지 구분하는 데 쓴다.
 *
 * @param query          점검한 검색어
 * @param interpretation 검색어를 어떻게 해석했는지
 * @param candidates     후보 상품별 점수. 최종 결과에서 빠진 것도 이유와 함께 포함한다(점수 높은 순)
 */
public record FinancialSearchExplainResponse(
        String query,
        QueryInterpretation interpretation,
        List<Candidate> candidates
) {

    /**
     * 검색어 해석 결과(규칙 기반). 실제 검색에서는 이 신호가 약할 때 LLM 보강이 붙지만,
     * 점검 화면에서는 원인을 명확히 보려고 규칙 기반 해석만 보여준다.
     *
     * @param productTypeIntent  감지된 상품유형 의도(SAVINGS/LOAN). 없으면 null이고 두 유형 모두 후보가 된다
     * @param demographicGroups  감지된 인구집단(YOUTH, SENIOR 등). 비어 있으면 인구집단 필터를 적용하지 않는다
     * @param age                검색어에서 뽑아낸 나이. 없으면 null
     * @param expandedTerms      동의어까지 확장해 실제로 DB를 조회한 검색어 목록
     * @param vectorSearchUsed   벡터검색이 실제로 동작했는지. false면 키워드 점수만으로 순위가 정해진다
     * @param vectorHitCount     벡터검색이 유사도 임계값을 넘겨 찾아낸 문서 수
     * @param minimumSimilarity  적용된 유사도 임계값
     */
    public record QueryInterpretation(
            String productTypeIntent,
            List<String> demographicGroups,
            Integer age,
            List<String> expandedTerms,
            boolean vectorSearchUsed,
            int vectorHitCount,
            double minimumSimilarity
    ) {
    }

    /**
     * 후보 상품 1건의 채점 내역.
     *
     * @param semanticScore  벡터 유사도(0~1). 벡터검색에 안 걸렸으면 null
     * @param lexicalScore   키워드 일치 점수(일치한 단어 수 ÷ 검색어 단어 수)
     * @param totalScore     최종 점수 = 유사도×0.4 + 키워드×0.6
     * @param matchedTerms   실제로 상품 텍스트에서 일치한 검색어
     * @param included       최종 결과에 포함됐는지
     * @param excludedReason 빠졌다면 그 이유. 포함됐으면 null
     */
    public record Candidate(
            String targetType,
            Long productId,
            String companyName,
            String productName,
            Double semanticScore,
            double lexicalScore,
            double totalScore,
            List<String> matchedTerms,
            boolean included,
            String excludedReason
    ) {
    }
}
