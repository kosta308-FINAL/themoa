package com.weaone.themoa.domain.logging.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * Micrometer {@code Timer.max()}는 기본 설정(distributionStatisticExpiry=2분)상 최근 구간만 반영하는
 * decaying(rolling) 값이라, 오래된 호출의 큰 값은 시간이 지나면 사라져 평균(count/totalTime은 영구 누적)보다
 * 작아지는 역전이 생길 수 있다. 서버 실행 내내 유지되는 진짜 lifetime max를 여기서 직접 추적한다.
 */
@Component
public class ApiMaxResponseTimeTracker {

    private final ConcurrentHashMap<String, LongAccumulator> maxMsByKey = new ConcurrentHashMap<>();

    public void record(String method, String uri, String status, long durationMs) {
        maxMsByKey
                .computeIfAbsent(key(method, uri, status), k -> new LongAccumulator(Long::max, 0))
                .accumulate(durationMs);
    }

    public long getMaxMs(String method, String uri, String status) {
        LongAccumulator accumulator = maxMsByKey.get(key(method, uri, status));
        return accumulator == null ? 0 : accumulator.get();
    }

    private String key(String method, String uri, String status) {
        return method + " " + uri + " " + status;
    }
}
