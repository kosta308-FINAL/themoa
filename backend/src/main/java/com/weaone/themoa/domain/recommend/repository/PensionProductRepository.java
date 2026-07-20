package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.recommend.entity.PensionProduct;
import com.weaone.themoa.domain.recommend.entity.PensionProductOption;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 연금저축 상품 Repository.
 * 배치 Upsert 시 product_code로 기존 상품을 조회한다(연금저축은 product_type 구분 없음).
 * 옵션(PensionProductOption)은 cascade로 함께 저장되므로 별도 Repository를 두지 않는다.
 */
public interface PensionProductRepository extends JpaRepository<PensionProduct, Long> {

    /** 배치 Upsert용 - 회사코드+상품코드로 기존 상품 조회(있으면 갱신, 없으면 신규 저장). */
    Optional<PensionProduct> findByCompanyCodeAndProductCode(String companyCode, String productCode);

    /** 존재 여부만 필요할 때. */
    boolean existsByCompanyCodeAndProductCode(String companyCode, String productCode);
}
