package com.weaone.themoa.domain.category.repository;

import com.weaone.themoa.domain.category.entity.CategoryKeywordRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryKeywordRuleRepository extends JpaRepository<CategoryKeywordRule, Long> {

    /**
     * 가맹점명 원본에 키워드가 부분일치(포함)하는 규칙을 매칭 우선순위로 정렬해 반환한다(category.md §5).
     * priority가 지정된 규칙이 먼저(오름차순), 나머지는 키워드 길이 내림차순("쿠팡이츠">"쿠팡")이다.
     * 호출자는 첫 번째 결과만 사용한다.
     */
    @Query("select r from CategoryKeywordRule r "
            + "where :merchantNameRaw like concat('%', r.keyword, '%') "
            + "order by case when r.priority is null then 1 else 0 end, r.priority asc, length(r.keyword) desc")
    List<CategoryKeywordRule> findMatchingOrderByPriority(@Param("merchantNameRaw") String merchantNameRaw);
}
