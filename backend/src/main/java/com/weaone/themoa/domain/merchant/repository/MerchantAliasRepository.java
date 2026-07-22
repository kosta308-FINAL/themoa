package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantAliasRepository extends JpaRepository<MerchantAlias, Long> {

    /** "서비스 1개 = alias 1행" 중복 방지용 조회. trim + uppercase로만 비교한다(merchant.md §1). */
    @Query("select a from MerchantAlias a where upper(trim(a.canonicalServiceName)) = upper(trim(:canonicalServiceName))")
    Optional<MerchantAlias> findByCanonicalServiceNameNormalized(@Param("canonicalServiceName") String canonicalServiceName);

    /**
     * F-03 가맹점 검색 드롭다운(view/fixedExpense.md §3.3). 검색어가 없을 때의 초기 목록(상위 N건은
     * {@code pageable}로 제한, mustrule §6-9 무제한 조회 금지). 컨트롤러에서 세션 밖에
     * {@code MerchantAliasResponse.from}으로 변환하므로 defaultCategory를 미리 fetch join 한다.
     */
    @Query("""
            select a from MerchantAlias a
            left join fetch a.defaultCategory
            order by a.canonicalServiceName asc
            """)
    List<MerchantAlias> findByOrderByCanonicalServiceNameAsc(Pageable pageable);

    /** F-03 가맹점 검색 드롭다운. 부분 일치 검색, 이름순 상위 N건만 준다. 위와 같은 이유로 defaultCategory를 fetch join 한다. */
    @Query("""
            select a from MerchantAlias a
            left join fetch a.defaultCategory
            where upper(a.canonicalServiceName) like upper(concat('%', :query, '%'))
            order by a.canonicalServiceName asc
            """)
    List<MerchantAlias> findByCanonicalServiceNameContainingIgnoreCaseOrderByCanonicalServiceNameAsc(
            @Param("query") String query, Pageable pageable);
}
