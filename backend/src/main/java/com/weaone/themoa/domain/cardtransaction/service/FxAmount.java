package com.weaone.themoa.domain.cardtransaction.service;

import java.math.BigDecimal;

/** 해외결제 원화 환산 결과(cardtransaction.md §4). 국내 결제는 originalAmount=null, exchangeRate=null이다. */
public record FxAmount(BigDecimal amount, BigDecimal originalAmount, String currencyCode,
                        BigDecimal exchangeRate, boolean exchangeRateEstimated) {
}
