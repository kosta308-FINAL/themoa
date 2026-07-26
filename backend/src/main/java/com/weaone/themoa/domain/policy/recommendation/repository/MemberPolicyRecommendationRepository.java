package com.weaone.themoa.domain.policy.recommendation.repository;

import com.weaone.themoa.domain.policy.recommendation.entity.MemberPolicyRecommendation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberPolicyRecommendationRepository extends JpaRepository<MemberPolicyRecommendation, Long> {
    @EntityGraph(attributePaths = {"policy", "policy.condition", "policy.regions", "policy.regions.region"})
    List<MemberPolicyRecommendation> findByMember_IdOrderByScoreDescGeneratedAtDesc(Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("delete from MemberPolicyRecommendation recommendation where recommendation.member.id = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}
