package com.weaone.themoa.common.exception;

import com.weaone.themoa.common.logging.ControllerNameResolver;
import com.weaone.themoa.common.logging.ErrorLogSanitizer;
import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.logging.dto.UnexpectedErrorEvent;
import com.weaone.themoa.domain.logging.service.AsyncErrorLogRecorder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

/**
 * 예상 오류와 예상하지 못한 오류를 분리한다(managelogging.md §0-3). {@code error_log}에는 최종적으로
 * 500으로 응답한 경우만 남긴다 — {@link #handleUnexpected}가 잡은 예외, 그리고 {@link #handleBusiness}가
 * 잡았지만 {@code ErrorCode}가 500으로 정의된 {@link BusinessException}(사용자 입력과 무관한 서버 내부
 * 실패를 의도적으로 BusinessException으로 표현한 경우) 둘 다다. 4xx BusinessException·Validation
 * 실패·401/403은 DB 에러 이벤트를 발행하지 않는다.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorLogSanitizer errorLogSanitizer;
    private final AsyncErrorLogRecorder asyncErrorLogRecorder;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("서버 내부 실패로 정의된 BusinessException. errorCode={}", errorCode.name(), e);
            String exceptionClass = "BusinessException:" + errorCode.name();
            asyncErrorLogRecorder.record(buildEvent(
                    e, request, currentMemberId(), exceptionClass, errorCode.getStatus().value()));
        }
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.name(), errorCode.getMessage()));
    }

    /**
     * Bean Validation 실패. 어떤 필드가 왜 틀렸는지는 프론트가 표시해야 하므로 첫 위반 메시지를 내려준다.
     * 검증 메시지에는 입력값 원문을 넣지 않는다(비밀번호·이메일 노출 방지).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name(), message));
    }

    /** 요청 바디 JSON 파싱 실패. 사용자 입력 오류라 400으로 응답하고 DB 에러 이벤트를 발행하지 않는다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name(), ErrorCode.INVALID_INPUT.getMessage()));
    }

    /** 파일당 10MB·요청당 30MB(customerservice.md §5)를 넘는 multipart 요청. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        ErrorCode errorCode = ErrorCode.CUSTOMER_INQUIRY_FILE_LIMIT_EXCEEDED;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.name(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("처리하지 못한 예외", e);
        asyncErrorLogRecorder.record(
                buildEvent(
                        e,
                        request,
                        currentMemberId(),
                        e.getClass().getName(),
                        ErrorCode.INTERNAL_ERROR.getStatus().value()));
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    private Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long memberId) {
            return memberId;
        }
        return null;
    }

    private UnexpectedErrorEvent buildEvent(Exception e, HttpServletRequest request, Long memberId,
                                             String exceptionClass, int statusCode) {
        return new UnexpectedErrorEvent(
                MDC.get("traceId"),
                memberId,
                request.getMethod(),
                request.getRequestURI(),
                ControllerNameResolver.resolve(request),
                statusCode,
                exceptionClass,
                errorLogSanitizer.sanitizeMessage(e.getMessage()),
                errorLogSanitizer.sanitizeStackTrace(e),
                LocalDateTime.now()
        );
    }
}
