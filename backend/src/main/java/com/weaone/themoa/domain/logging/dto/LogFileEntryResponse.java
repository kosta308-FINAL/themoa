package com.weaone.themoa.domain.logging.dto;

/** 관리자 파일 로그 뷰어 한 줄. {@code logback-spring.xml}의 key=value 패턴을 파싱한 결과다. */
public record LogFileEntryResponse(
        String timestamp,
        String level,
        String traceId,
        String thread,
        String logger,
        String message
) {
}
