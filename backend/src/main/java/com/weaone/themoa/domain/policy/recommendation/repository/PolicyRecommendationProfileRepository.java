package com.weaone.themoa.domain.policy.recommendation.repository;

import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRecommendationProfileRepository extends JpaRepository<PolicyRecommendationProfile, Long> {
    @EntityGraph(attributePaths = {"member"})
    Optional<PolicyRecommendationProfile> findByMember_Id(Long memberId);

    @Override
    @EntityGraph(attributePaths = {"member"})
    Page<PolicyRecommendationProfile> findAll(Pageable pageable);

    boolean existsByMember_Id(Long memberId);
}
