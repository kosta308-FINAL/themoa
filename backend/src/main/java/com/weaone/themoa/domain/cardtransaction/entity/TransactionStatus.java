package com.weaone.themoa.domain.cardtransaction.entity;

/** 거래상태(cardtransaction.md §3-1). {@code resCancelYN} 4값과 1:1로 대응한다. */
public enum TransactionStatus {
    APPROVED,
    CANCELED,
    PARTIAL_CANCELED,
    REJECTED
}
