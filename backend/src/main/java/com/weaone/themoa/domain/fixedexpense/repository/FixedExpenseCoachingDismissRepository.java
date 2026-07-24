package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCoachingDismiss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixedExpenseCoachingDismissRepository extends JpaRepository<FixedExpenseCoachingDismiss, Long> {

    List<FixedExpenseCoachingDismiss> findByMember_Id(Long memberId);

    boolean existsByMember_IdAndFixedExpense_Id(Long memberId, Long fixedExpenseId);
}
