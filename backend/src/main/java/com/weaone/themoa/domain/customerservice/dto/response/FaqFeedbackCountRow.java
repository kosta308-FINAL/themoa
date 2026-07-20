package com.weaone.themoa.domain.customerservice.dto.response;

/** FAQ 목록 조회 시 피드백 집계 배치 조회 결과 1행(faqId, helpful 여부, 건수). */
public record FaqFeedbackCountRow(Long faqId, boolean helpful, long count) {
}
