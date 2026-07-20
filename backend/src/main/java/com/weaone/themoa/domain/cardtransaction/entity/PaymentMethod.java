package com.weaone.themoa.domain.cardtransaction.entity;

/** 결제수단(erd.md 거래내역). 자동이체는 별도 값을 두지 않고 {@code TRANSFER}에 통합한다. */
public enum PaymentMethod {
    CARD,
    CASH,
    TRANSFER
}
