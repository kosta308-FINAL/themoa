package com.weaone.themoa.domain.fixedexpense.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * LLM이 이미 대상으로 고른 후보인데 문구가 숫자 무결성 검사를 통과하지 못했을 때만 쓰는 폴백(LLM 호출
 * 자체가 실패하면 이 팩토리를 타지 않는다 — 대상 선정까지 템플릿이 대신할 안전한 규칙이 없어서다).
 * 항상 담담한 "연 환산" 문장만 조립한다.
 */
@Component
public class FixedExpenseCoachingTemplateCardFactory {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,###");

    public FixedExpenseCoachingDraft create(FixedExpenseCoachingCandidate candidate) {
        String title = candidate.name() + " 연 환산 금액";
        String body = String.format(
                "%s는 한 달 %s원이지만, 1년으로 보면 %s원이에요.",
                candidate.name(), format(candidate.monthlyAmount()), format(candidate.annualAmount()));
        return new FixedExpenseCoachingDraft(candidate.targetRef(), title, body);
    }

    private String format(BigDecimal amount) {
        return AMOUNT_FORMAT.format(amount);
    }
}
