package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.entity.PolicyEmbeddingSync;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PolicyEmbeddingSyncRepository extends JpaRepository<PolicyEmbeddingSync, Long> {
    Optional<PolicyEmbeddingSync> findByPolicyId(Integer policyId);

    long countBySyncStatus(String syncStatus);

    @Query("select s.policy.id from PolicyEmbeddingSync s where s.syncStatus = :status order by s.requestedAt asc")
    List<Integer> findPolicyIdsByStatus(@Param("status") String status, Pageable pageable);

    @EntityGraph(attributePaths = "policy")
    @Query("""
            select s from PolicyEmbeddingSync s
            where (:status is null or s.syncStatus = :status)
              and (:keyword is null or lower(s.policy.title) like lower(concat('%', :keyword, '%'))
                   or lower(s.policy.sourcePolicyId) like lower(concat('%', :keyword, '%')))
            """)
    Page<PolicyEmbeddingSync> searchForAdmin(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);
}
