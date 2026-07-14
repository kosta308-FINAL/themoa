package com.weaone.themoa.domain.cardtransaction.service;

/**
 * 30일 초과 미이용자의 복귀 동기화 선택지(cardtransaction.md §6-C). 서버가 날짜를 계산하며,
 * 클라이언트는 임의의 시작일을 보내지 않고 이 두 모드만 보낸다.
 */
public enum RecoveryMode {
    /** 복귀일이 속한 달의 1일부터(기본). 과거 공백은 채우지 않는다. */
    CURRENT_MONTH,
    /** MAX(커넥션별 마지막 성공 동기화일, 복귀 월 1일 − 3개월)부터. */
    RECOVER_RECENT
}
