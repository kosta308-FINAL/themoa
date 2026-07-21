package com.weaone.themoa.domain.financialsearch.repository;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 금융검색(financialsearch) 전용 조회 레포지토리. 상품 엔티티는 recommend 도메인 것을 재사용하되,
// 검색용 키워드 쿼리는 이 도메인에서 별도로 정의한다(recommend 배치용 레포지토리와 책임 분리).
public interface FinancialSavingsSearchRepository extends JpaRepository<SavingsProduct, Long> {

    // Qdrant 벡터 검색이 꺼져있거나 결과가 없을 때 쓰는 폴백 검색(단순 LIKE).
    // closeDate is null = 아직 판매중인 상품만. join fetch로 options를 같이 가져와 N+1을 피함.
    @Query("""
            select distinct p from SavingsProduct p
            left join fetch p.options
            where p.closeDate is null
            and (lower(p.productName) like lower(concat('%', :keyword, '%'))
                 or lower(p.joinTarget) like lower(concat('%', :keyword, '%'))
                 or lower(p.specialCondition) like lower(concat('%', :keyword, '%'))
                 or lower(p.companyName) like lower(concat('%', :keyword, '%')))
            """)
    List<SavingsProduct> searchByKeyword(@Param("keyword") String keyword);
}
