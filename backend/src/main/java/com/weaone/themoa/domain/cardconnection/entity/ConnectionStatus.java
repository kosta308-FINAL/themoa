package com.weaone.themoa.domain.cardconnection.entity;

/**
 * ERROR와 LOCKED를 구분하는 이유는 사용자가 해야 할 일이 다르기 때문이다(connection.md §ERD).
 * ERROR = 앱에서 재연결하면 풀린다 / LOCKED = 카드사 계정 잠금이라 우리가 풀 수 없고 사용자가 카드사에서 직접 풀어야 한다.
 * DISCONNECTED = 사용자가 마이페이지에서 직접 연동을 해제했다. 같은 카드사로 다시 연결하면 이 행이 재사용된다.
 */
public enum ConnectionStatus {
    ACTIVE,
    ERROR,
    LOCKED,
    DISCONNECTED
}
