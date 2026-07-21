package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantAliasTermsRepository extends JpaRepository<MerchantAliasTerms, Long> {

    /** 이 회원이 학습시킨 표기 중 완전일치(trim+uppercase)하는 것. 내 term이 전역보다 우선한다(merchant.md §2-1). */
    @Query("select t from MerchantAliasTerms t "
            + "where t.member.id = :memberId and upper(trim(t.aliasText)) = upper(trim(:rawName))")
    Optional<MerchantAliasTerms> findMineByRawName(@Param("memberId") Long memberId, @Param("rawName") String rawName);

    /** 관리자 전역 시드 표기 중 완전일치(trim+uppercase)하는 것. */
    @Query("select t from MerchantAliasTerms t "
            + "where t.member is null and upper(trim(t.aliasText)) = upper(trim(:rawName))")
    Optional<MerchantAliasTerms> findGlobalByRawName(@Param("rawName") String rawName);

    Optional<MerchantAliasTerms> findByMember_IdAndAliasText(Long memberId, String aliasText);

    /** 관리자 서비스 병합 대상 조회. */
    List<MerchantAliasTerms> findByMerchantAlias_Id(Long merchantAliasId);

    /**
     * 관리자 전역 승격 대기목록(manage.html "전역 마스터 승격 대기목록" 확장). 회원 학습 표기를
     * (alias, 표기) 단위로 묶어 학습자 수 내림차순으로 준다. {@code defaultCategory}가 없는 alias도
     * 빠지지 않도록 left join으로 카테고리를 가져온다. 이미 전역 등록됐거나 관리자가 반려한
     * (alias, 표기) 조합은 다시 안 뜨도록 둘 다 제외한다.
     */
    @Query("select t.merchantAlias.id as aliasId, t.aliasText as aliasText, "
            + "a.canonicalServiceName as canonicalServiceName, c.name as categoryName, "
            + "count(distinct t.member.id) as learnerCount "
            + "from MerchantAliasTerms t "
            + "join t.merchantAlias a "
            + "left join a.defaultCategory c "
            + "where t.member is not null "
            + "and not exists ("
            + "  select g.id from MerchantAliasTerms g "
            + "  where g.member is null "
            + "  and upper(trim(g.aliasText)) = upper(trim(t.aliasText))"
            + ") "
            + "and not exists ("
            + "  select r.id from PromotionCandidateRejection r "
            + "  where r.merchantAlias.id = t.merchantAlias.id "
            + "  and upper(trim(r.aliasText)) = upper(trim(t.aliasText))"
            + ") "
            + "group by t.merchantAlias.id, t.aliasText, a.canonicalServiceName, c.name "
            + "order by count(distinct t.member.id) desc")
    List<PromotionCandidate> findPromotionCandidates(Pageable pageable);

    interface PromotionCandidate {
        Long getAliasId();
        String getAliasText();
        String getCanonicalServiceName();
        String getCategoryName();
        long getLearnerCount();
    }
}
