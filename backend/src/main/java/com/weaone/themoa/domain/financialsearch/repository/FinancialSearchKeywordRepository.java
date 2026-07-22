package com.weaone.themoa.domain.financialsearch.repository;

import com.weaone.themoa.domain.financialsearch.entity.FinancialSearchKeyword;
import com.weaone.themoa.domain.financialsearch.entity.SearchKeywordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialSearchKeywordRepository extends JpaRepository<FinancialSearchKeyword, Long> {

    List<FinancialSearchKeyword> findByKeywordTypeOrderByGroupKeyAscKeywordAsc(SearchKeywordType keywordType);

    boolean existsByKeywordTypeAndGroupKeyAndKeyword(SearchKeywordType keywordType, String groupKey, String keyword);
}
