package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByMember_IdAndYearMonth(Long memberId, String yearMonth);

    Optional<Budget> findByIdAndMember_Id(Long id, Long memberId);

    /** 주기 마감 배치 대상: 종료일이 지난(오늘 이전) 완료 주기(dailyBudget.md §4, MOA-S-BUD-BGT-11). */
    List<Budget> findByCycleEndDateBefore(LocalDate date);
}
