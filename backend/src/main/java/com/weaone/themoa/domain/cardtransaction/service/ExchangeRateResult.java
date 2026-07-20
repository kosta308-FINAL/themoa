package com.weaone.themoa.domain.cardtransaction.service;

import java.math.BigDecimal;

/**
 * 환율 조회 결과. {@code estimated}가 true면 API 장애로 캐시의 직전 성공 환율(폴백)을 쓴 것이라
 * {@code card_transaction.exchange_rate_estimated}에 그대로 반영한다(cardtransaction.md §4).
 */
public record ExchangeRateResult(BigDecimal rate, boolean estimated) {
}
