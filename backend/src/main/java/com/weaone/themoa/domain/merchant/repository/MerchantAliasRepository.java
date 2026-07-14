package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MerchantAliasRepository extends JpaRepository<MerchantAlias, Long> {

    /** "서비스 1개 = alias 1행" 중복 방지용 조회. trim + uppercase로만 비교한다(merchant.md §1). */
    @Query("select a from MerchantAlias a where upper(trim(a.canonicalServiceName)) = upper(trim(:canonicalServiceName))")
    Optional<MerchantAlias> findByCanonicalServiceNameNormalized(@Param("canonicalServiceName") String canonicalServiceName);
}
