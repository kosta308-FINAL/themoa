package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import com.weaone.themoa.domain.recommend.entity.LoanProductOption;
import com.weaone.themoa.domain.recommend.entity.LoanType;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 대출 상품 Repository.
 * 배치 Upsert 시 product_code + product_type(주택담보/전세자금/개인신용)으로 기존 상품을 조회한다.
 * 옵션(LoanProductOption)은 cascade로 함께 저장되므로 별도 Repository를 두지 않는다.
 */
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    /** 배치 Upsert용 - 회사코드+상품코드+유형으로 기존 상품 조회(있으면 갱신, 없으면 신규 저장). */
    Optional<LoanProduct> findByCompanyCodeAndProductCodeAndProductType(
            String companyCode, String productCode, LoanType productType);

    /** 존재 여부만 필요할 때. */
    boolean existsByCompanyCodeAndProductCodeAndProductType(
            String companyCode, String productCode, LoanType productType);
}
