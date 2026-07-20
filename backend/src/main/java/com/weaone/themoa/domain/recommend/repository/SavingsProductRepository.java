package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import com.weaone.themoa.domain.recommend.entity.SavingsType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
