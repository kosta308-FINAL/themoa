package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.PromotionCandidateRejection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionCandidateRejectionRepository extends JpaRepository<PromotionCandidateRejection, Long> {

    boolean existsByMerchantAlias_IdAndAliasText(Long merchantAliasId, String aliasText);
}
