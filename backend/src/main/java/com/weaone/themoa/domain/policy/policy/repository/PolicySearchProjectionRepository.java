package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PolicySearchProjectionRepository extends JpaRepository<PolicySearchProjection, Integer> {
    Optional<PolicySearchProjection> findByPolicyId(Integer policyId);
    long countByProjectionVersion(String projectionVersion);
    long countByMissingSnapshotTrue();

    @Query("select p from PolicySearchProjection p join fetch p.policy policy where policy.active = true")
    List<PolicySearchProjection> findAllActive();

    @Query("select max(p.updatedAt) from PolicySearchProjection p where p.projectionVersion = :projectionVersion")
    java.time.LocalDateTime findLastUpdatedAtByProjectionVersion(String projectionVersion);
}
