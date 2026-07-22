package com.weaone.themoa.domain.financialsearch.repository;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /** 판매중인 예·적금에 등장하는 금융회사명. 관리자 화면에서 "공식 링크가 없는 은행"을 찾는 데 쓴다. */
    @Query("""
            select distinct p.companyName from SavingsProduct p
            where p.closeDate is null and p.companyName is not null
            """)
    List<String> findDistinctCompanyNames();

    /** 판매중(공시종료일 없음) 상품 수. */
    long countByCloseDateIsNull();

    /**
     * 가장 최근에 갱신된 상품의 시각. 수집 배치가 upsert하며 updated_at을 갱신하므로
     * 별도 실행 이력 테이블 없이 "마지막 수집 시각"의 근사치로 쓴다.
     */
    @Query("select max(p.updatedAt) from SavingsProduct p")
    LocalDateTime findLastUpdatedAt();
}
