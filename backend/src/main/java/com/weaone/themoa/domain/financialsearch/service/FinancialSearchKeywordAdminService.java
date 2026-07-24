package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.dto.request.FinancialSearchKeywordAddRequest;
import com.weaone.themoa.domain.financialsearch.dto.response.FinancialSearchKeywordListResponse;
import com.weaone.themoa.domain.financialsearch.entity.FinancialSearchKeyword;
import com.weaone.themoa.domain.financialsearch.entity.SearchKeywordType;
import com.weaone.themoa.domain.financialsearch.repository.FinancialSearchKeywordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 검색어 해석 키워드 관리(관리자 전용).
 *
 * <p>변경 후에는 반드시 {@link FinancialSearchKeywordProvider#refresh()}를 불러 캐시를 갱신한다.
 * 그러지 않으면 추가·삭제해도 서버를 재시작하기 전까지 검색에 반영되지 않는다.
 */
@Service
public class FinancialSearchKeywordAdminService {

    private final FinancialSearchKeywordRepository keywordRepository;
    private final FinancialSearchKeywordProvider keywordProvider;

    public FinancialSearchKeywordAdminService(FinancialSearchKeywordRepository keywordRepository,
                                              FinancialSearchKeywordProvider keywordProvider) {
        this.keywordRepository = keywordRepository;
        this.keywordProvider = keywordProvider;
    }

    @Transactional(readOnly = true)
    public FinancialSearchKeywordListResponse findAll() {
        return new FinancialSearchKeywordListResponse(
                loadGroups(SearchKeywordType.DEMOGRAPHIC),
                loadGroups(SearchKeywordType.PRODUCT_INTENT));
    }

    /**
     * 키워드 추가. 같은 그룹에 같은 단어가 이미 있으면 아무것도 하지 않는다(멱등).
     *
     * @return 이번 호출로 새로 추가했으면 true
     */
    @Transactional
    public boolean add(FinancialSearchKeywordAddRequest request) {
        String groupKey = request.groupKey().trim().toUpperCase();
        String keyword = request.keyword().trim();

        if (keywordRepository.existsByKeywordTypeAndGroupKeyAndKeyword(request.keywordType(), groupKey, keyword)) {
            return false;
        }
        keywordRepository.save(FinancialSearchKeyword.of(request.keywordType(), groupKey, keyword));
        keywordProvider.refresh();
        return true;
    }

    /** 키워드 삭제. 이미 없는 id여도 오류 없이 넘어간다. */
    @Transactional
    public void delete(Long keywordId) {
        keywordRepository.deleteById(keywordId);
        keywordProvider.refresh();
    }

    /** 기본값으로 초기화. 관리자가 편집한 내용을 모두 지우고 코드에 정의된 기본 키워드로 되돌린다. */
    @Transactional
    public FinancialSearchKeywordListResponse resetToDefaults() {
        keywordRepository.deleteAllInBatch();
        keywordRepository.saveAll(keywordProvider.defaultRows());
        keywordProvider.refresh();
        return findAll();
    }

    private List<FinancialSearchKeywordListResponse.KeywordGroup> loadGroups(SearchKeywordType type) {
        Map<String, List<FinancialSearchKeywordListResponse.KeywordItem>> grouped = new LinkedHashMap<>();
        for (FinancialSearchKeyword row : keywordRepository.findByKeywordTypeOrderByGroupKeyAscKeywordAsc(type)) {
            grouped.computeIfAbsent(row.getGroupKey(), key -> new ArrayList<>())
                    .add(new FinancialSearchKeywordListResponse.KeywordItem(row.getId(), row.getKeyword()));
        }
        return grouped.entrySet().stream()
                .map(entry -> new FinancialSearchKeywordListResponse.KeywordGroup(entry.getKey(), entry.getValue()))
                .toList();
    }
}
