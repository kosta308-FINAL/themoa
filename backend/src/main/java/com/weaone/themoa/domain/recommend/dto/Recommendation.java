package com.weaone.themoa.domain.recommend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 추천 결과 1건.
 *
 * @param id          상품 PK(savings_product.id). 추천 화면에서 북마크할 때 대상 식별자로 쓴다.
 * @param company     금융회사명
 * @param productName 상품명
 * @param type        예금/적금 (DEPOSIT/SAVING)
 * @param score        소프트필터 총점
 * @param bestRate     대표(최고)금리
 * @param bestRateTerm 그 금리의 가입기간(개월). 회전정기예금은 회전주기 기준
 * @param reasons      점수를 받은 사유 목록(추천 이유 재료)
 * @param llmReason    LLM이 생성한 자연어 추천 이유(규칙 기반이면 null)
 * @param maturityAmountWon 사용자 월납입가능액(여력) 전액을 넣었을 때 만기 총 수령액(원금+이자, 세전 추정).
 *                          적금(SAVING)만 계산됨(예금은 월납입 개념이 안 맞아 null).
 * @param goalMonthlyWon    저축목표가 있을 때, 그 목표만 채우려면 매월 얼마씩 넣으면 되는지(여력보다 적을 수 있음).
 *                          목표 없거나 예금이면 null.
 * @param goalMaturityAmountWon goalMonthlyWon으로 넣었을 때 만기 총 수령액(목표금액과 비슷하거나 약간 더 큼).
 */
public record Recommendation(
        Long id,
        String company,
        String productName,
        String type,
        int score,
        BigDecimal bestRate,
        Integer bestRateTerm,
        List<String> reasons,
        String llmReason,
        Long maturityAmountWon,
        Long goalMonthlyWon,
        Long goalMaturityAmountWon
) {
    /** llmReason만 바꾼 복사본. */
    public Recommendation withLlmReason(String reason) {
        return new Recommendation(id, company, productName, type, score, bestRate, bestRateTerm, reasons, reason,
                maturityAmountWon, goalMonthlyWon, goalMaturityAmountWon);
    }
}
