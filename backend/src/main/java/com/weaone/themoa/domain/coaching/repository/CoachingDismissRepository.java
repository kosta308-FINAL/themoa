package com.weaone.themoa.domain.coaching.repository;

import com.weaone.themoa.domain.coaching.entity.CoachingDismiss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoachingDismissRepository extends JpaRepository<CoachingDismiss, Long> {

    List<CoachingDismiss> findByMember_Id(Long memberId);

    Optional<CoachingDismiss> findByMember_IdAndCategory_Id(Long memberId, Long categoryId);

    Optional<CoachingDismiss> findByMember_IdAndMerchantAlias_Id(Long memberId, Long merchantAliasId);
}
