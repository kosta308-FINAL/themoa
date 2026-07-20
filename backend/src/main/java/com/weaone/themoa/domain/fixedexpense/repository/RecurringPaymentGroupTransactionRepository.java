package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroupTransaction;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroupTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringPaymentGroupTransactionRepository
        extends JpaRepository<RecurringPaymentGroupTransaction, RecurringPaymentGroupTransactionId> {
}
