package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {

    /** 컨트롤러에서 세션 밖에 {@code FixedExpenseResponse.from}으로 변환하므로 category/merchantAlias를 미리 fetch join 한다. */
    @Query("""
            select f from FixedExpense f
            join fetch f.category
            left join fetch f.merchantAlias
            where f.id = :id and f.member.id = :memberId
            """)
    Optional<FixedExpense> findByIdAndMember_Id(Long id, Long memberId);

    /** 위와 동일한 이유로 fetch join 한다(fixedExpense.md §4 목록 조회). */
    @Query("""
            select f from FixedExpense f
            join fetch f.category
            left join fetch f.merchantAlias
            where f.member.id = :memberId and f.status = :status
            """)
    List<FixedExpense> findByMember_IdAndStatus(Long memberId, FixedExpenseStatus status);

    /** 수집 매칭 조건①(fixedExpense.md §5) — 이름 기반 alias 대조. */
    List<FixedExpense> findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(
            Long memberId, Long merchantAliasId, FixedExpenseStatus status, FixedExpensePaymentMethod paymentMethod);

    /** 수집 매칭 조건① — biller 경유 대조. */
    List<FixedExpense> findByMember_IdAndBillerMerchant_IdAndStatusAndPaymentMethod(
            Long memberId, Long billerMerchantId, FixedExpenseStatus status, FixedExpensePaymentMethod paymentMethod);

    /** 새벽 배치(미납·예정일 알림) 전체 스캔용. */
    List<FixedExpense> findByStatus(FixedExpenseStatus status);

    List<FixedExpense> findByMember_IdAndStatusAndExpectedPayDayIsNotNull(Long memberId, FixedExpenseStatus status);
}
