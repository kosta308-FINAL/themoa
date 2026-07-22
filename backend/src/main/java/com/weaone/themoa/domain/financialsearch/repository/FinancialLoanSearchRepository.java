package com.weaone.themoa.domain.financialsearch.repository;

import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// FinancialSavingsSearchRepository.searchByKeyword와 동일한 목적의 SQL 키워드 폴백 검색.
// 대출은 special_condition이 항상 비어있는 데이터라(finlife API 특성), note/loan_limit까지 같이 본다.
public interface FinancialLoanSearchRepository extends JpaRepository<LoanProduct, Long> {

    @Query("""
            select distinct p from LoanProduct p
            left join fetch p.options
            where p.closeDate is null
            and (lower(p.productName) like lower(concat('%', :keyword, '%'))
                 or lower(p.specialCondition) like lower(concat('%', :keyword, '%'))
                 or lower(p.companyName) like lower(concat('%', :keyword, '%'))
                 or lower(p.note) like lower(concat('%', :keyword, '%'))
                 or lower(p.loanLimit) like lower(concat('%', :keyword, '%')))
            """)
    List<LoanProduct> searchByKeyword(@Param("keyword") String keyword);

    /** 판매중인 대출에 등장하는 금융회사명. 관리자 화면에서 "공식 링크가 없는 은행"을 찾는 데 쓴다. */
    @Query("""
            select distinct p.companyName from LoanProduct p
            where p.closeDate is null and p.companyName is not null
            """)
    List<String> findDistinctCompanyNames();

    /** 판매중(공시종료일 없음) 상품 수. */
    long countByCloseDateIsNull();

    /** {@link FinancialSavingsSearchRepository#findLastUpdatedAt()}의 대출 버전. */
    @Query("select max(p.updatedAt) from LoanProduct p")
    LocalDateTime findLastUpdatedAt();
}
