package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.entity.FinancialSearchKeyword;
import com.weaone.themoa.domain.financialsearch.entity.SearchKeywordType;
import com.weaone.themoa.domain.financialsearch.repository.FinancialSearchKeywordRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 검색어 해석 키워드를 검색 로직에 공급한다.
 *
 * <p>매 검색마다 DB를 읽지 않도록 캐시에 담아두고, 관리자가 변경하면 {@link #refresh()}로 다시 읽는다
 * (은행 링크 캐시와 같은 방식). 캐시를 안 갱신하면 등록해도 재시작 전까지 반영되지 않는다.
 *
 * <p>기동 시 테이블이 비어 있으면 {@link FinancialSearchKeywordDefaults}로 채운다. 그래야 관리자가
 * 키워드 하나를 추가했을 때 나머지 기본 키워드가 사라지는 일이 없다.
 */
@Component
public class FinancialSearchKeywordProvider {

    private final FinancialSearchKeywordRepository keywordRepository;

    // 요청 스레드가 변경 직후의 값을 바로 보도록 volatile.
    private volatile Map<String, List<String>> demographicGroups = Map.of();
    private volatile List<String> savingsIntentKeywords = List.of();
    private volatile List<String> loanIntentKeywords = List.of();

    public FinancialSearchKeywordProvider(FinancialSearchKeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    @PostConstruct
    void initialize() {
        seedIfEmpty();
        refresh();
    }

    /** 관리자가 키워드를 추가·삭제·초기화한 뒤 호출한다. */
    public void refresh() {
        this.demographicGroups = loadGroups(SearchKeywordType.DEMOGRAPHIC);
        Map<String, List<String>> intents = loadGroups(SearchKeywordType.PRODUCT_INTENT);
        this.savingsIntentKeywords = intents.getOrDefault(FinancialSearchKeywordDefaults.GROUP_SAVINGS, List.of());
        this.loanIntentKeywords = intents.getOrDefault(FinancialSearchKeywordDefaults.GROUP_LOAN, List.of());
    }

    /** 인구집단 그룹키 → 키워드 목록. */
    public Map<String, List<String>> demographicGroups() {
        return demographicGroups;
    }

    public List<String> savingsIntentKeywords() {
        return savingsIntentKeywords;
    }

    public List<String> loanIntentKeywords() {
        return loanIntentKeywords;
    }

    /** 테이블이 비어 있을 때만 기본값을 넣는다(이미 관리자가 손댔으면 건드리지 않는다). */
    void seedIfEmpty() {
        if (keywordRepository.count() > 0) {
            return;
        }
        keywordRepository.saveAll(defaultRows());
    }

    /** "기본값으로 초기화"용 — 전체 삭제 후 기본값으로 다시 채운다. */
    List<FinancialSearchKeyword> defaultRows() {
        List<FinancialSearchKeyword> rows = new ArrayList<>();
        FinancialSearchKeywordDefaults.DEMOGRAPHIC_GROUPS.forEach((group, keywords) ->
                keywords.forEach(keyword ->
                        rows.add(FinancialSearchKeyword.of(SearchKeywordType.DEMOGRAPHIC, group, keyword))));
        FinancialSearchKeywordDefaults.SAVINGS_INTENT_KEYWORDS.forEach(keyword ->
                rows.add(FinancialSearchKeyword.of(SearchKeywordType.PRODUCT_INTENT,
                        FinancialSearchKeywordDefaults.GROUP_SAVINGS, keyword)));
        FinancialSearchKeywordDefaults.LOAN_INTENT_KEYWORDS.forEach(keyword ->
                rows.add(FinancialSearchKeyword.of(SearchKeywordType.PRODUCT_INTENT,
                        FinancialSearchKeywordDefaults.GROUP_LOAN, keyword)));
        return rows;
    }

    private Map<String, List<String>> loadGroups(SearchKeywordType type) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (FinancialSearchKeyword row : keywordRepository.findByKeywordTypeOrderByGroupKeyAscKeywordAsc(type)) {
            grouped.computeIfAbsent(row.getGroupKey(), key -> new ArrayList<>()).add(row.getKeyword());
        }
        return grouped;
    }
}
