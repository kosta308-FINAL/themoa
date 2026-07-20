package com.weaone.themoa.domain.cardconnection.client;

/** CODEF 통신·타임아웃·파싱 실패. 원문 오류·자격정보는 담지 않는다(mustrule §7-8). */
public class CodefClientException extends RuntimeException {

    public CodefClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
