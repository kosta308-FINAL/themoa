package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, Integer> {
    List<PolicyRegion> findByPolicy(Policy policy);
}
