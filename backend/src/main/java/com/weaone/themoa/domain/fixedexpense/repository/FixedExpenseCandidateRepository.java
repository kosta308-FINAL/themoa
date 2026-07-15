package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedExpenseCandidateRepository extends JpaRepository<FixedExpenseCandidate, Long> {

    Optional<FixedExpenseCandidate> findByRecurringPaymentGroup_Id(Long recurringGroupId);

    Optional<FixedExpenseCandidate> findByIdAndMember_Id(Long id, Long memberId);

    List<FixedExpenseCandidate> findByMember_IdAndStatus(Long memberId, FixedExpenseCandidateStatus status);

    List<FixedExpenseCandidate> findByStatus(FixedExpenseCandidateStatus status);
}
