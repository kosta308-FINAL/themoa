package com.weaone.themoa.domain.category.repository;

import com.weaone.themoa.domain.category.entity.MerchantTypeCategoryMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantTypeCategoryMapRepository extends JpaRepository<MerchantTypeCategoryMap, Long> {

    /** 업종 매핑은 완전일치만 쓴다(category.md §4 — 업종 오염이라 부분일치는 위험). */
    Optional<MerchantTypeCategoryMap> findByMerchantType(String merchantType);
}
