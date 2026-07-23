package com.weaone.themoa.domain.financialchange.repository;

import com.weaone.themoa.domain.financialchange.entity.FinancialWatchSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialWatchSnapshotRepository extends JpaRepository<FinancialWatchSnapshot, Long> {

    List<FinancialWatchSnapshot> findByMemberId(Long memberId);
}
