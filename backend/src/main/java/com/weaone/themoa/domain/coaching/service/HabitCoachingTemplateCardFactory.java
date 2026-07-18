package com.weaone.themoa.domain.coaching.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * LLM 없이도 항상 조립 가능한 폴백 카드(habitExpense.md §4). 규칙 계층이 이미 모든 숫자를 계산해두므로
 * 문구만 고정 템플릿으로 채운다 — "카드 없음"으로 가지 않기 위한 바닥선이다.
 */
@Component
public class HabitCoachingTemplateCardFactory {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,###");

    public CoachingCardDraft create(HabitCoachingCandidate candidate) {
        String title = candidate.label() + " 소비가 눈에 띄어요";
        String body = candidate.toneDown()
                ? buildToneDownBody(candidate)
                : buildDefaultBody(candidate);
        return new CoachingCardDraft(candidate.targetRef(), title, body);
    }

    private String buildDefaultBody(HabitCoachingCandidate candidate) {
        return String.format(
                "%s에 이번 주기 %d번, 월 평균 약 %s원을 쓰고 있어요. 필수 소비일 수도 있지만, 줄일 여지가 있다면 월 %s원 정도 아낄 수 있어요.",
                candidate.label(), candidate.transactionCount(), format(candidate.monthlyAverage()),
                format(candidate.estimatedSaving()));
    }

    /** 사용자가 이전에 "필요한 소비"로 표시한 대상(NOT_WASTE)은 단정 없이 정보만 담담하게 전한다. */
    private String buildToneDownBody(HabitCoachingCandidate candidate) {
        return String.format(
                "%s에 이번 주기 %d번, 월 평균 약 %s원을 쓰고 있어요. 필요한 소비로 표시해 주신 항목이라 참고로만 보여드려요.",
                candidate.label(), candidate.transactionCount(), format(candidate.monthlyAverage()));
    }

    private String format(BigDecimal amount) {
        return AMOUNT_FORMAT.format(amount);
    }
}
