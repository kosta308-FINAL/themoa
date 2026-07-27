package com.weaone.themoa.domain.category.repository;

import com.weaone.themoa.domain.category.entity.MerchantTypeCategoryMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantTypeCategoryMapRepository extends JpaRepository<MerchantTypeCategoryMap, Long> {

    /** 업종 매핑은 완전일치만 쓴다(category.md §4 — 업종 오염이라 부분일치는 위험). */
    Optional<MerchantTypeCategoryMap> findByMerchantType(String merchantType);

    /** 시더가 "없는 것만 추가"할 때 쓰는 존재 확인(완전일치). */
    boolean existsByMerchantType(String merchantType);
}
