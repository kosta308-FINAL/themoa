package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.domain.RegionSyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionSyncRunRepository extends JpaRepository<RegionSyncRun, Long> {
    Optional<RegionSyncRun> findTopByOrderByStartedAtDesc();
}
