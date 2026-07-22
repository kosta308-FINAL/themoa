package com.weaone.themoa.common.logging;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 에러 메시지·StackTrace 비식별화(managelogging.md §3-3). Request Body·Query String은 이 클래스의
 * 입력으로 전달하지 않는다 — 애플리케이션 예외 메시지·StackTrace만 받는다.
 */
@Component
public class ErrorLogSanitizer {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;
    private static final int STACK_TRACE_MAX_LENGTH = 4000;
    private static final int STACK_TRACE_MAX_FRAMES = 20;

    private static final Pattern AUTHORIZATION_HEADER =
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*)\\S+");
    private static final Pattern BEARER_TOKEN =
            Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9\\-_.+/=]+");
    private static final Pattern JWT_LIKE =
            Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern TOKEN_OR_KEY =
            Pattern.compile("(?i)((?:access[_-]?token|refresh[_-]?token|api[_-]?key)\\s*[:=]\\s*)[^\\s,;]+");
    private static final Pattern PASSWORD =
            Pattern.compile("(?i)(password\\s*[:=]\\s*)\\S+");
    private static final Pattern COOKIE =
            Pattern.compile("(?i)(cookie\\s*[:=]\\s*)[^;\\n]+");
    private static final Pattern VERIFICATION_CODE =
            Pattern.compile("(?i)((?:verification|인증)[_\\- ]?code\\s*[:=]\\s*)\\S+");
    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CARD_NUMBER =
            Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    /** 원문이 null이면 null. 그 외에는 마스킹 후 최대 1,000자로 자른다. */
    public String sanitizeMessage(String rawMessage) {
        if (rawMessage == null) {
            return null;
        }
        return truncate(mask(rawMessage), ERROR_MESSAGE_MAX_LENGTH);
    }

    /** 예외 클래스·비식별화된 메시지와 상위 20개 frame까지만 문자열로 만들고 최대 4,000자로 자른다. */
    public String sanitizeStackTrace(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwable.getClass().getName())
                .append(": ")
                .append(mask(throwable.getMessage() == null ? "null" : throwable.getMessage()));

        StackTraceElement[] elements = throwable.getStackTrace();
        int frameCount = Math.min(elements.length, STACK_TRACE_MAX_FRAMES);
        for (int i = 0; i < frameCount; i++) {
            builder.append("\n\tat ").append(elements[i]);
        }
        return truncate(builder.toString(), STACK_TRACE_MAX_LENGTH);
    }

    private String mask(String value) {
        String masked = value;
        masked = AUTHORIZATION_HEADER.matcher(masked).replaceAll("$1****");
        masked = BEARER_TOKEN.matcher(masked).replaceAll("$1****");
        masked = JWT_LIKE.matcher(masked).replaceAll("****");
        masked = TOKEN_OR_KEY.matcher(masked).replaceAll("$1****");
        masked = PASSWORD.matcher(masked).replaceAll("$1****");
        masked = COOKIE.matcher(masked).replaceAll("$1****");
        masked = VERIFICATION_CODE.matcher(masked).replaceAll("$1****");
        masked = EMAIL.matcher(masked).replaceAll("****");
        masked = CARD_NUMBER.matcher(masked).replaceAll("****");
        return masked;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
