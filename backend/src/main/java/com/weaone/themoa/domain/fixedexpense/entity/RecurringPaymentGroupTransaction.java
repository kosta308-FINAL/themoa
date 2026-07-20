package com.weaone.themoa.domain.fixedexpense.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반복결제 그룹 ↔ 거래 연결 테이블(erd.md §5). 복합 PK 자체가 "같은 거래를 같은 그룹에 두 번 안 넣는다"는
 * 중복 방지다. 탐지 배치가 그 주기에 그룹 판정 근거로 삼은 거래들을 기록한다(감사·재계산용).
 */
@Entity
@Table(name = "recurring_payment_group_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringPaymentGroupTransaction {

    @EmbeddedId
    private RecurringPaymentGroupTransactionId id;

    private RecurringPaymentGroupTransaction(RecurringPaymentGroupTransactionId id) {
        this.id = id;
    }

    public static RecurringPaymentGroupTransaction of(Long recurringGroupId, Long transactionId) {
        return new RecurringPaymentGroupTransaction(
                new RecurringPaymentGroupTransactionId(recurringGroupId, transactionId));
    }
}
