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

    /**
     * 관리자 "서비스 전체 목록 & 병합" 화면(merchant.md 확장). 등록 경로가 여러 곳(F-03 검색, 직접 등록 시
     * 자유 입력)이라 같은 실서비스가 이름만 다르게 여러 행으로 쌓일 수 있어, 관리자가 눈으로 훑어 중복을
     * 찾을 수 있도록 이름순 전체 목록 + 사용 건수를 함께 준다.
     */
    @Query("select a.id as aliasId, a.canonicalServiceName as canonicalServiceName, c.name as categoryName, "
            + "(select count(fe) from FixedExpense fe where fe.merchantAlias = a) as fixedExpenseCount, "
            + "(select count(m) from Merchant m where m.merchantAlias = a) as merchantCount "
            + "from MerchantAlias a left join a.defaultCategory c "
            + "order by a.canonicalServiceName asc")
    List<AliasUsageSummary> findAllWithUsage();

    interface AliasUsageSummary {
        Long getAliasId();
        String getCanonicalServiceName();
        String getCategoryName();
        long getFixedExpenseCount();
        long getMerchantCount();
    }
}
