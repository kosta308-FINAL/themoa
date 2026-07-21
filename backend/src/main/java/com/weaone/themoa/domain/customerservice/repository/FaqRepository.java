package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    Optional<Faq> findByIdAndActiveTrue(Long id);

    /**
     * 사용자 노출용 FAQ 검색(customerservice.md §4-1). 활성 카테고리·활성 FAQ만 대상이고,
     * categoryCode·keyword는 없으면(null) 조건에서 제외한다. 정렬은 호출자가 Pageable로 지정한다.
     */
    @Query("""
            select f from Faq f
            join fetch f.faqCategory fc
            where f.active = true
              and fc.active = true
              and (:categoryCode is null or fc.code = :categoryCode)
              and (:keyword is null
                   or lower(f.question) like concat('%', :keyword, '%')
                   or lower(f.answerMarkdown) like concat('%', :keyword, '%'))
            """)
    Page<Faq> search(@Param("categoryCode") String categoryCode, @Param("keyword") String keyword, Pageable pageable);
}
