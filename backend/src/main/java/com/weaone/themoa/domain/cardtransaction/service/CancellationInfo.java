package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;

import java.math.BigDecimal;

/** {@code resCancelYN} 해석 결과(cardtransaction.md §3). */
public record CancellationInfo(TransactionStatus status, BigDecimal canceledAmount, boolean cancelAmountUncertain) {
}
