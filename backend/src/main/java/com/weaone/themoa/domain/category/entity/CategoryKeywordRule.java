package com.weaone.themoa.domain.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가맹점명 키워드 → 카테고리 규칙(category.md §5). 부분일치(포함) 매칭이며, 신원 판별의 완전일치
 * ({@code merchant_alias_terms})와는 규칙이 다르므로 절대 통합하지 않는다.
 * 매칭 순서는 {@code priority} 지정 규칙이 먼저(오름차순), 나머지는 키워드 길이 내림차순이다.
 */
@Entity
@Table(name = "category_keyword_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryKeywordRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String keyword;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** 길이 정렬로 안 풀리는 예외만 채운다. 대부분의 규칙은 비워 두고 길이 정렬에 맡긴다(§5). */
    @Column(name = "priority")
    private Integer priority;

    private CategoryKeywordRule(String keyword, Category category, Integer priority) {
        this.keyword = keyword;
        this.category = category;
        this.priority = priority;
    }

    public static CategoryKeywordRule seed(String keyword, Category category) {
        return new CategoryKeywordRule(keyword, category, null);
    }

    public static CategoryKeywordRule seedWithPriority(String keyword, Category category, int priority) {
        return new CategoryKeywordRule(keyword, category, priority);
    }
}
