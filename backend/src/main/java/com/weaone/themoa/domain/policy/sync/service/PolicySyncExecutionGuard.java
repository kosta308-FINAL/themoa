package com.weaone.themoa.domain.policy.sync.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PolicySyncExecutionGuard {
    // 현재 단일 애플리케이션 인스턴스 기준으로 정책 작업 중복 실행을 막는다.
    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    public void release() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }
}
