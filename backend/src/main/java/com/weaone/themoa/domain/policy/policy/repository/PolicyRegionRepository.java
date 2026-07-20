package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, Integer> {
    List<PolicyRegion> findByPolicy(Policy policy);
}
