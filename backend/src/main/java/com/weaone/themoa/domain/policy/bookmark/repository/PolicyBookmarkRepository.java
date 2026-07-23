package com.weaone.themoa.domain.policy.bookmark.repository;

import com.weaone.themoa.domain.policy.bookmark.entity.PolicyBookmark;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PolicyBookmarkRepository extends JpaRepository<PolicyBookmark, Integer> {

    @EntityGraph(attributePaths = {"policy"})
    Optional<PolicyBookmark> findByMember_IdAndPolicy_Id(Long memberId, Integer policyId);

    @EntityGraph(attributePaths = {"policy"})
    List<PolicyBookmark> findByMember_IdOrderByIdDesc(Long memberId);

    long deleteByMember_IdAndPolicy_Id(Long memberId, Integer policyId);

    @EntityGraph(attributePaths = {"policy"})
    @Query("""
            select b from PolicyBookmark b
            where b.member.id = :memberId
              and (
                b.policy.startDate between :startDate and :endDate
                or b.policy.dueDate between :startDate and :endDate
              )
            """)
    List<PolicyBookmark> findCalendarTargets(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
