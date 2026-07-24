package com.weaone.themoa.domain.policy.recommendation.repository;

import com.weaone.themoa.domain.policy.recommendation.entity.MemberPolicyRecommendation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberPolicyRecommendationRepository extends JpaRepository<MemberPolicyRecommendation, Long> {
    @EntityGraph(attributePaths = {"policy", "policy.condition", "policy.regions", "policy.regions.region"})
    List<MemberPolicyRecommendation> findByMember_IdOrderByScoreDescGeneratedAtDesc(Long memberId);

    void deleteByMember_Id(Long memberId);
}
