package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.entity.PolicyRawData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRawDataRepository extends JpaRepository<PolicyRawData, Long> {
    Optional<PolicyRawData> findTopBySourceAndSourcePolicyIdOrderByCollectedAtDesc(String source, String sourcePolicyId);
}
