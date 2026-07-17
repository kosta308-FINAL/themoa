package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.SurplusFund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurplusFundRepository extends JpaRepository<SurplusFund, Long> {

    boolean existsByMember_IdAndYearMonth(Long memberId, String yearMonth);
}
