package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.Faq;

/** FAQ 목록 항목(customerservice.md §4-1). helpfulCount·unhelpfulCount는 {@code faq_feedback} 집계값이다. */
public record FaqResponse(
        Long id,
        String categoryCode,
        String categoryName,
        String question,
        String answerMarkdown,
        long helpfulCount,
        long unhelpfulCount,
        Boolean myFeedback
) {

    public static FaqResponse of(Faq faq, long helpfulCount, long unhelpfulCount, Boolean myFeedback) {
        return new FaqResponse(
                faq.getId(),
                faq.getFaqCategory().getCode(),
                faq.getFaqCategory().getName(),
                faq.getQuestion(),
                faq.getAnswerMarkdown(),
                helpfulCount,
                unhelpfulCount,
                myFeedback
        );
    }
}
