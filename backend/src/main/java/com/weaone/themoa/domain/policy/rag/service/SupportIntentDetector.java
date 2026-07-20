package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 사용자 질의와 구조화 조건에서 지원 형태 의도를 추출한다.
 *
 * <p>정책 분야(SearchDomain)와 지원 형태(SupportIntent)는 서로 다른 축이다. "지원금"은 금융 분야를
 * 의미하지 않고 현금성 지원 방식만 의미할 수 있으므로, 이 클래스에서 문자열 supportTypes,
 * 원문 질의, positiveKeywords를 하나의 SupportIntent 집합으로 정규화한다.</p>
 */
@Component
public class SupportIntentDetector {
    public Detection detect(String query, Set<String> supportTypes, Set<String> positiveKeywords) {
        Set<SupportIntent> intents = new LinkedHashSet<>();
        Set<String> evidence = new LinkedHashSet<>();
        detectFromText("query", query, intents, evidence);
        for (String value : safe(supportTypes)) {
            detectFromText("condition.supportTypes", value, intents, evidence);
        }
        for (String value : safe(positiveKeywords)) {
            detectFromText("positiveKeywords", value, intents, evidence);
        }
        return new Detection(Set.copyOf(intents), evidence.stream().toList());
    }

    public Set<SupportIntent> fromDomains(Set<SearchDomain> domains) {
        Set<SupportIntent> intents = new LinkedHashSet<>();
        for (SearchDomain domain : domains == null ? Set.<SearchDomain>of() : domains) {
            switch (domain) {
                case EMPLOYMENT -> intents.add(SupportIntent.EMPLOYMENT_SUPPORT);
                case HOUSING -> intents.add(SupportIntent.HOUSING_COST);
                case EDUCATION -> intents.add(SupportIntent.EDUCATION);
                case FINANCE -> {
                    intents.add(SupportIntent.ASSET_BUILDING);
                    intents.add(SupportIntent.SAVINGS);
                    intents.add(SupportIntent.MATCHED_SAVINGS);
                    intents.add(SupportIntent.LOAN);
                }
                default -> {
                }
            }
        }
        return intents;
    }

    private void detectFromText(String source, String raw, Set<SupportIntent> intents, Set<String> evidence) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String text = raw.replaceAll("\\s+", "");
        if (containsAny(text, "CASH", "CASH_ASSISTANCE", "현금", "현금지원", "금전지원", "금전적혜택", "지원금",
                "받을수있는돈", "돈으로지원", "현금으로주는혜택", "교통비", "대중교통", "환급", "페이백", "할인",
                "비용지원", "비용경감", "바우처", "K-패스", "K패스", "k-패스", "k패스", "케이패스")) {
            add(intents, evidence, SupportIntent.CASH_ASSISTANCE, source, raw);
        }
        if (containsAny(text, "ALLOWANCE", "수당", "생활수당", "활동수당")) {
            add(intents, evidence, SupportIntent.CASH_ASSISTANCE, source, raw);
            add(intents, evidence, SupportIntent.ALLOWANCE, source, raw);
        }
        if (containsAny(text, "SUBSIDY", "보조금", "장려금")) {
            add(intents, evidence, SupportIntent.CASH_ASSISTANCE, source, raw);
        }
        if (containsAny(text, "LOAN", "대출", "융자", "이자지원", "이자보전")) {
            add(intents, evidence, SupportIntent.LOAN, source, raw);
        }
        if (containsAny(text, "SAVINGS", "저축", "계좌", "통장")) {
            add(intents, evidence, SupportIntent.SAVINGS, source, raw);
        }
        if (containsAny(text, "MATCHED_SAVINGS", "매칭적립", "매칭해", "매칭지원", "정부기여금", "본인저축액추가적립")) {
            add(intents, evidence, SupportIntent.SAVINGS, source, raw);
            add(intents, evidence, SupportIntent.MATCHED_SAVINGS, source, raw);
        }
        if (containsAny(text, "HOUSING_COST", "월세", "주거비", "임차료")) {
            add(intents, evidence, SupportIntent.HOUSING_COST, source, raw);
        }
    }

    private void add(Set<SupportIntent> intents, Set<String> evidence, SupportIntent intent, String source, String raw) {
        intents.add(intent);
        evidence.add(source + ": " + raw);
    }

    private Set<String> safe(Set<String> values) {
        return values == null ? Set.of() : values;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    public record Detection(Set<SupportIntent> intents, java.util.List<String> evidence) {
    }
}
