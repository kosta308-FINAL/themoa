package com.weaone.themoa.domain.cardtransaction.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 회원 단위 in-flight 락(cardtransaction.md §6-1). CODEF는 같은 카드사 계정(connectedId+organization)에
 * 대한 다건 요청을 하나의 로그인 세션으로 순차 처리하므로, 같은 회원에게 온디맨드·배치 동기화가 동시에
 * 걸리면 세션 충돌·중복요청 에러가 난다. 단일 인스턴스 전제(메모리 락) — 다중 인스턴스 배포 시 분산 락으로 교체한다.
 */
@Component
public class CardSyncLockService {

    private final Set<Long> lockedMemberIds = ConcurrentHashMap.newKeySet();

    public boolean tryAcquire(Long memberId) {
        return lockedMemberIds.add(memberId);
    }

    public void release(Long memberId) {
        lockedMemberIds.remove(memberId);
    }
}
