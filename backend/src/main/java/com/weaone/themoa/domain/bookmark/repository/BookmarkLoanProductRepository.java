package com.weaone.themoa.domain.bookmark.repository;

import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** {@link BookmarkSavingsProductRepository}의 대출 버전. 대표금리(최저금리) 계산용 options를 함께 가져온다. */
public interface BookmarkLoanProductRepository extends JpaRepository<LoanProduct, Long> {

    @Query("""
            select distinct p from LoanProduct p
            left join fetch p.options
            where p.id in :ids
            """)
    List<LoanProduct> findAllWithOptionsByIdIn(@Param("ids") Collection<Long> ids);
}
