package com.weaone.themoa.domain.policy.bookmark.repository;

import com.weaone.themoa.domain.policy.bookmark.entity.PolicyBookmark;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyBookmarkRepository extends JpaRepository<PolicyBookmark, Integer> {

    @EntityGraph(attributePaths = {"policy"})
    Optional<PolicyBookmark> findByMember_IdAndPolicy_Id(Long memberId, Integer policyId);

    @EntityGraph(attributePaths = {"policy"})
    List<PolicyBookmark> findByMember_IdOrderByIdDesc(Long memberId);

    long deleteByMember_IdAndPolicy_Id(Long memberId, Integer policyId);
}
