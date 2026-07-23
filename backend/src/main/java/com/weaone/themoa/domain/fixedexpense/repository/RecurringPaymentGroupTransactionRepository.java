package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroupTransaction;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroupTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecurringPaymentGroupTransactionRepository
        extends JpaRepository<RecurringPaymentGroupTransaction, RecurringPaymentGroupTransactionId> {

    /**
     * alias형 그룹 find-or-create의 신원 판단 기준(fixedExpense.md §2): 금액이 아니라 이 그룹에 이미
     * 링크된 증거 거래와의 겹침으로 "같은 구독의 재탐지인지"를 가른다 — 같은 alias 안에 값이 비슷한
     * 별개 구독이 있으면 금액으론 못 갈라서다.
     */
    @Query("select case when count(rpgt) > 0 then true else false end from RecurringPaymentGroupTransaction rpgt "
            + "where rpgt.id.recurringGroupId = :groupId and rpgt.id.transactionId in :transactionIds")
    boolean existsOverlap(@Param("groupId") Long groupId, @Param("transactionIds") List<Long> transactionIds);
}
