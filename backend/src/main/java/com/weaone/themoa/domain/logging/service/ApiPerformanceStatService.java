package com.weaone.themoa.domain.logging.service;

import com.weaone.themoa.domain.logging.dto.ApiPerformanceStatResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 엔드포인트별 응답시간(ms)을 요청마다 DB에 쌓지 않고, Micrometer가 메모리에 누적해 둔
 * {@code http.server.requests} 타이머를 읽어서 집계 결과만 보여준다.
 */
@Service
@RequiredArgsConstructor
public class ApiPerformanceStatService {

    private final MeterRegistry meterRegistry;

    public List<ApiPerformanceStatResponse> readStats(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return meterRegistry.find("http.server.requests").timers().stream()
                .map(this::toResponse)
                .filter(stat -> matchesKeyword(stat, normalizedKeyword))
                .sorted(Comparator.comparingDouble(ApiPerformanceStatResponse::avgMs).reversed())
                .toList();
    }

    private ApiPerformanceStatResponse toResponse(Timer timer) {
        long count = timer.count();
        double totalMs = timer.totalTime(TimeUnit.MILLISECONDS);
        double avgMs = count == 0 ? 0 : totalMs / count;
        return new ApiPerformanceStatResponse(
                tag(timer, "method"),
                tag(timer, "uri"),
                tag(timer, "status"),
                count,
                round(avgMs),
                round(timer.max(TimeUnit.MILLISECONDS)));
    }

    private String tag(Timer timer, String key) {
        String value = timer.getId().getTag(key);
        return value == null ? "-" : value;
    }

    private boolean matchesKeyword(ApiPerformanceStatResponse stat, String normalizedKeyword) {
        if (normalizedKeyword.isEmpty()) {
            return true;
        }
        return stat.uri().toLowerCase().contains(normalizedKeyword)
                || stat.method().toLowerCase().contains(normalizedKeyword);
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
