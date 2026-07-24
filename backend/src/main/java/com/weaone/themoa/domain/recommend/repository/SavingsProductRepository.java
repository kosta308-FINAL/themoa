package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import com.weaone.themoa.domain.recommend.entity.SavingsType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 예·적금 상품 Repository.
 * 배치 Upsert 시 product_code + product_type(예금/적금)으로 기존 상품을 조회한다.
 * 옵션(SavingsProductOption)은 cascade로 함께 저장되므로 별도 Repository를 두지 않는다.
 */
public interface SavingsProductRepository extends JpaRepository<SavingsProduct, Long> {

    /** 배치 Upsert용 - 회사코드+상품코드+유형으로 기존 상품 조회(있으면 갱신, 없으면 신규 저장). */
    Optional<SavingsProduct> findByCompanyCodeAndProductCodeAndProductType(
            String companyCode, String productCode, SavingsType productType);

    /** 존재 여부만 필요할 때. */
    boolean existsByCompanyCodeAndProductCodeAndProductType(
            String companyCode, String productCode, SavingsType productType);

    /** 추천용 - 판매중(close_date 없음) 상품을 옵션까지 함께(fetch join) 조회. */
    @Query("select distinct p from SavingsProduct p left join fetch p.options where p.closeDate is null")
    List<SavingsProduct> findAllSellingWithOptions();

    /** 우대조건 캐시 배치용 - 판매중이면서 우대조건 원문이 있는 상품의 id·원문만 가볍게 조회. */
    @Query("""
            select p.id as id, p.specialCondition as specialCondition
            from SavingsProduct p
            where p.closeDate is null and p.specialCondition is not null and p.specialCondition <> ''
            """)
    List<ProductConditionView> findSellingConditions();

    /** {@link #findSellingConditions()} 프로젝션. */
    interface ProductConditionView {
        Long getId();
        String getSpecialCondition();
    }

    /**
     * 관리자 우대조건 관리용 - 판매중이면서 우대조건 원문이 있는 상품을 은행/상품명 키워드로 검색.
     * keyword가 비면 전체(원문 있는 상품)를 은행·상품명 순으로 돌려준다.
     */
    @Query("""
            select p.id as id, p.companyName as companyName,
                   p.productName as productName, p.productType as productType
            from SavingsProduct p
            where p.closeDate is null
              and p.specialCondition is not null and p.specialCondition <> ''
              and (:keyword is null or :keyword = ''
                   or p.companyName like concat('%', :keyword, '%')
                   or p.productName like concat('%', :keyword, '%'))
            order by p.companyName, p.productName
            """)
    List<ProductConditionSummaryView> searchConditionProducts(@Param("keyword") String keyword);

    /** {@link #searchConditionProducts(String)} 프로젝션. */
    interface ProductConditionSummaryView {
        Long getId();
        String getCompanyName();
        String getProductName();
        SavingsType getProductType();
    }
}
