package com.weaone.themoa.domain.cardtransaction.entity;

/**
 * 수집출처(erd.md 거래내역). {@code SYNC}=카드 연동 자동 수집, {@code MANUAL}=수기 입력(entryMode.md 소관,
 * 이 모듈에서는 생성 로직을 만들지 않는다). 벤더명(CODEF)이 아니라 "자동/수기" 여부를 답하는 값이다.
 */
public enum TransactionSource {
    SYNC,
    MANUAL
}
