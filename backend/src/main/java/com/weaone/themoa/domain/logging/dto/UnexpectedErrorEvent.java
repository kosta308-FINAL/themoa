package com.weaone.themoa.domain.logging.dto;

import java.time.LocalDateTime;

/**
 * 예상하지 못한 500 한 건의 비동기 저장 이벤트(managelogging.md §3-2). {@code HttpServletRequest},
 * {@code Authentication}, {@code Exception} 객체 자체를 담지 않고, 요청 종료 뒤에도 안전한 값만 담는다.
 */
public record UnexpectedErrorEvent(
        String traceId,
        Long memberId,
        String httpMethod,
        String requestUri,
        String controller,
        int statusCode,
        String exceptionClass,
        String errorMessage,
        String stackTraceExcerpt,
        LocalDateTime occurredAt
) {
}
