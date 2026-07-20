package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
