package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.BudgetIncomeAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BudgetIncomeAdjustmentRepository extends JpaRepository<BudgetIncomeAdjustment, Long> {

    List<BudgetIncomeAdjustment> findByBudget_IdOrderByCreatedAtDesc(Long budgetId);

    @Query("select coalesce(sum(a.amount), 0) from BudgetIncomeAdjustment a where a.budget.id = :budgetId")
    BigDecimal sumAmountByBudget_Id(@Param("budgetId") Long budgetId);

    /** 삭제 시 소유권 검증(본인 budget 소속인지)까지 한 번에 확인한다. */
    Optional<BudgetIncomeAdjustment> findByIdAndBudget_Member_Id(Long id, Long memberId);
}
