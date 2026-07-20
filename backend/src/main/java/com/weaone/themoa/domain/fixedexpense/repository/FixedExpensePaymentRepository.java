package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface FixedExpensePaymentRepository extends JpaRepository<FixedExpensePayment, Long> {

    boolean existsByFixedExpense_IdAndYearMonth(Long fixedExpenseId, String yearMonth);

    Optional<FixedExpensePayment> findByFixedExpense_IdAndYearMonth(Long fixedExpenseId, String yearMonth);

    /** 취소 재수집 시 이행 기록 삭제 → 미납 복귀(fixedExpense.md §7)에 사용. */
    Optional<FixedExpensePayment> findByCardTransaction_Id(Long cardTransactionId);

    /** 주기 확정 고정지출 합계(참고용, erd.md §6 confirmed_fixed_expense_total). 주기 시작 시점엔 결제 전이라 0이다. */
    @Query("select coalesce(sum(p.paidAmount), 0) from FixedExpensePayment p "
            + "where p.fixedExpense.member.id = :memberId and p.yearMonth = :yearMonth")
    BigDecimal sumPaidAmount(@Param("memberId") Long memberId, @Param("yearMonth") String yearMonth);
}
