package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.domain.RegionSyncError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionSyncErrorRepository extends JpaRepository<RegionSyncError, Long> {
}
