package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.SurplusFundTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface SurplusFundTransferRepository extends JpaRepository<SurplusFundTransfer, Long> {

    @Query("""
            select coalesce(sum(t.amount), 0)
            from SurplusFundTransfer t
            where t.budget.member.id = :memberId
            """)
    BigDecimal sumAmountByMember_Id(@Param("memberId") Long memberId);

    @Query("select coalesce(sum(t.amount), 0) from SurplusFundTransfer t where t.budget.id = :budgetId")
    BigDecimal sumAmountByBudget_Id(@Param("budgetId") Long budgetId);
}
