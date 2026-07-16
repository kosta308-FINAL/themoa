package com.weaone.themoa.domain.fixedexpense.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class RecurringPaymentGroupTransactionId implements Serializable {

    private Long recurringGroupId;
    private Long transactionId;

    public RecurringPaymentGroupTransactionId(Long recurringGroupId, Long transactionId) {
        this.recurringGroupId = recurringGroupId;
        this.transactionId = transactionId;
    }
}
