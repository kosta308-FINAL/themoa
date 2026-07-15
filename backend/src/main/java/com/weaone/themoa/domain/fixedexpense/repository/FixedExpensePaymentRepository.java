package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FixedExpensePaymentRepository extends JpaRepository<FixedExpensePayment, Long> {

    boolean existsByFixedExpense_IdAndYearMonth(Long fixedExpenseId, String yearMonth);

    Optional<FixedExpensePayment> findByFixedExpense_IdAndYearMonth(Long fixedExpenseId, String yearMonth);

    /** 취소 재수집 시 이행 기록 삭제 → 미납 복귀(fixedExpense.md §7)에 사용. */
    Optional<FixedExpensePayment> findByCardTransaction_Id(Long cardTransactionId);
}
