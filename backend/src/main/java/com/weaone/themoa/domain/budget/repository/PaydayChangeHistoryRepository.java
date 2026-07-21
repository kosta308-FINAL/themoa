package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.PaydayChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaydayChangeHistoryRepository extends JpaRepository<PaydayChangeHistory, Long> {

    List<PaydayChangeHistory> findByMember_IdOrderByEffectiveCycleStartDateAsc(Long memberId);
}
