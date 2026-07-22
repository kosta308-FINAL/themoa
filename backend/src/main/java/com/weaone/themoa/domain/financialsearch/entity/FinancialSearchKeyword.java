package com.weaone.themoa.domain.financialsearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검색어 해석용 키워드. 관리자가 화면에서 추가·삭제하면 재배포 없이 검색 동작이 바뀐다.
 *
 * <p>예: SENIOR 그룹에 "노후"를 추가하면, "노후 준비"로 검색했을 때 시니어 상품이 후보로 잡힌다.
 *
 * <p>테이블이 비어 있으면 기동 시 코드에 있는 기본 키워드로 채운다. 그래야 관리자가 단어 하나를
 * 추가했다고 해서 나머지 기본 키워드가 사라지는 일이 없다.
 */
@Entity
@Table(
        name = "financial_search_keyword",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_financial_search_keyword",
                columnNames = {"keyword_type", "group_key", "keyword"}),
        indexes = @Index(name = "idx_financial_search_keyword_type", columnList = "keyword_type, group_key"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialSearchKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "keyword_type", nullable = false, length = 20)
    private SearchKeywordType keywordType;

    /** 인구집단이면 YOUTH·SENIOR 등, 상품의도면 SAVINGS·LOAN. */
    @Column(name = "group_key", nullable = false, length = 30)
    private String groupKey;

    @Column(name = "keyword", nullable = false, length = 50)
    private String keyword;

    private FinancialSearchKeyword(SearchKeywordType keywordType, String groupKey, String keyword) {
        this.keywordType = keywordType;
        this.groupKey = groupKey;
        this.keyword = keyword;
    }

    public static FinancialSearchKeyword of(SearchKeywordType keywordType, String groupKey, String keyword) {
        return new FinancialSearchKeyword(keywordType, groupKey, keyword);
    }
}
