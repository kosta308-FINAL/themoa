package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantAliasRepository extends JpaRepository<MerchantAlias, Long> {

    /** "서비스 1개 = alias 1행" 중복 방지용 조회. trim + uppercase로만 비교한다(merchant.md §1). */
    @Query("select a from MerchantAlias a where upper(trim(a.canonicalServiceName)) = upper(trim(:canonicalServiceName))")
    Optional<MerchantAlias> findByCanonicalServiceNameNormalized(@Param("canonicalServiceName") String canonicalServiceName);

    /** F-03 가맹점 검색 드롭다운(view/fixedExpense.md §3.3). 검색어가 없을 때의 초기 목록. */
    List<MerchantAlias> findTop20ByOrderByCanonicalServiceNameAsc();

    /** F-03 가맹점 검색 드롭다운. 부분 일치 검색, 이름순 상위 20건만 준다(mustrule §6-9 무제한 조회 금지). */
    List<MerchantAlias> findTop20ByCanonicalServiceNameContainingIgnoreCaseOrderByCanonicalServiceNameAsc(String query);
}
