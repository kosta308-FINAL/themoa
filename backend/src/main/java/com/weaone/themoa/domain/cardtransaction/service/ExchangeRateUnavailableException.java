package com.weaone.themoa.domain.cardtransaction.service;

/**
 * 환율 캐시가 완전히 비어 있는 상태(최초 실행 + 즉시 API 장애)에서 환율을 구할 방법이 전혀 없을 때만 던진다
 * (cardtransaction.md §4). 호출자는 이 거래 하나만 건너뛰고 나머지 거래 수집은 계속 진행한다.
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String message) {
        super(message);
    }
}
