package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {

    Optional<FixedExpense> findByIdAndMember_Id(Long id, Long memberId);

    List<FixedExpense> findByMember_IdAndStatus(Long memberId, FixedExpenseStatus status);

    /** 수집 매칭 조건①(fixedExpense.md §5) — 이름 기반 alias 대조. */
    List<FixedExpense> findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(
            Long memberId, Long merchantAliasId, FixedExpenseStatus status, FixedExpensePaymentMethod paymentMethod);

    /** 수집 매칭 조건① — biller 경유 대조. */
    List<FixedExpense> findByMember_IdAndBillerMerchant_IdAndStatusAndPaymentMethod(
            Long memberId, Long billerMerchantId, FixedExpenseStatus status, FixedExpensePaymentMethod paymentMethod);

    /** 새벽 배치(미납·예정일 알림) 전체 스캔용. */
    List<FixedExpense> findByStatus(FixedExpenseStatus status);
}
