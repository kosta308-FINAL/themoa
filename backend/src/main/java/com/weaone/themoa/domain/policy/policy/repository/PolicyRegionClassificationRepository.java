package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.entity.PolicyRegionClassification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRegionClassificationRepository extends JpaRepository<PolicyRegionClassification, Integer> {
    long countByRegionScope(String regionScope);
    long countByClassifierVersion(String classifierVersion);
}
