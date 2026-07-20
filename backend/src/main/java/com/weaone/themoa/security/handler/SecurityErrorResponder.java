package com.weaone.themoa.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 필터 단계에서 발생한 오류를 컨트롤러와 동일한 응답 계약으로 내려준다.
 * 이 단계는 {@code @RestControllerAdvice}가 잡지 못하므로 여기서 직접 직렬화한다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponder {

    private final ObjectMapper objectMapper;

    public void respond(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(errorCode.name(), errorCode.getMessage())
        );
    }
}