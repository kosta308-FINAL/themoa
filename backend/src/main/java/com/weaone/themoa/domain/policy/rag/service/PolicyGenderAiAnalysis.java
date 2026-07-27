package com.weaone.themoa.domain.policy.rag.service;

public record PolicyGenderAiAnalysis(
        String audience,
        Boolean exclusive,
        Double confidence,
        String applicantScope,
        String evidence
) {
}
