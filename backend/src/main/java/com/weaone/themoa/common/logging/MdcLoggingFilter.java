package com.weaone.themoa.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 traceId를 부여하고 요청 완료를 구조화된 한 줄 로그로 남긴다(managelogging.md §2-1).
 * {@code SecurityConfig}에서 {@code JwtAuthenticationFilter}보다 앞에 등록해야, JWT 필터에서 바로 끝나는
 * 401도 같은 traceId로 파일 로그에 남고 응답 헤더를 받을 수 있다.
 */
@Slf4j
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String ANONYMOUS_MEMBER_ID = "anonymous";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            logRequestCompletion(request, response, durationMs);
            MDC.clear();
        }
    }

    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        int status = response.getStatus();
        String message = "event=http_request_completed method=%s uri=%s status=%d durationMs=%d memberId=%s controller=%s"
                .formatted(request.getMethod(), request.getRequestURI(), status, durationMs,
                        resolveMemberId(), ControllerNameResolver.resolve(request));
        if (status >= 500) {
            log.error(message);
        } else if (status >= 400) {
            log.warn(message);
        } else {
            log.info(message);
        }
    }

    /** 인증된 요청의 principal은 {@code JwtAuthenticationFilter}가 항상 Long memberId를 넣는다. */
    private String resolveMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long memberId) {
            return String.valueOf(memberId);
        }
        return ANONYMOUS_MEMBER_ID;
    }
}
