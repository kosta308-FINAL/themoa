package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Integer> {
    Optional<Policy> findBySourceTypeAndSourcePolicyId(String sourceType, String sourcePolicyId);

    Optional<Policy> findBySourcePolicyId(String sourcePolicyId);

    long countByActiveTrue();

    List<Policy> findByActiveTrue(Pageable pageable);

    @Query("""
            select distinct p
            from Policy p
            left join fetch p.condition
            left join fetch p.regions policyRegion
            left join fetch policyRegion.region
            where p.id in :ids
            """)
    List<Policy> findWithRelationsByIdIn(List<Integer> ids);

    @Query("select p.id from Policy p where p.active = true order by p.id asc")
    List<Integer> findActivePolicyIds(Pageable pageable);
}
