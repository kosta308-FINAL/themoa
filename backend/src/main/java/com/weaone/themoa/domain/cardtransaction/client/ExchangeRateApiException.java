package com.weaone.themoa.domain.cardtransaction.client;

/** 수출입은행 환율 API 호출 실패(타임아웃·HTTP 오류·파싱 실패). 인증코드·원문 오류는 메시지에 담지 않는다. */
public class ExchangeRateApiException extends RuntimeException {

    public ExchangeRateApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
