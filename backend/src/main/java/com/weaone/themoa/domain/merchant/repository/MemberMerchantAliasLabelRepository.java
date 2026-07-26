package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.MemberMerchantAliasLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberMerchantAliasLabelRepository extends JpaRepository<MemberMerchantAliasLabel, Long> {

    Optional<MemberMerchantAliasLabel> findByMember_IdAndMerchantAlias_Id(Long memberId, Long merchantAliasId);

    /** 거래 목록 표시명 일괄 치환용(N+1 방지) — 그 목록에 등장하는 alias들만 한 번에 조회한다. */
    List<MemberMerchantAliasLabel> findByMember_IdAndMerchantAlias_IdIn(Long memberId, Collection<Long> merchantAliasIds);

    void deleteByMember_IdAndMerchantAlias_Id(Long memberId, Long merchantAliasId);
}
