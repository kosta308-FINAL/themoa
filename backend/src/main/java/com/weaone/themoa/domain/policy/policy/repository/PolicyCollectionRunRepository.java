package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.domain.PolicyCollectionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyCollectionRunRepository extends JpaRepository<PolicyCollectionRun, Long> {
    Optional<PolicyCollectionRun> findTopByOrderByStartedAtDesc();
}
