package com.weaone.themoa.domain.policy.common.exception;

import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.weaone.themoa.domain.policy")
public class PolicyExceptionHandler {

    @ExceptionHandler(SearchDataNotReadyException.class)
    ResponseEntity<ApiResponse<Void>> handleSearchDataNotReady(SearchDataNotReadyException exception) {
        return error(ErrorCode.POLICY_SEARCH_NOT_READY);
    }

    @ExceptionHandler(YouthCenterApiResponseException.class)
    ResponseEntity<ApiResponse<Void>> handleYouthCenterApiResponse(YouthCenterApiResponseException exception) {
        return error(ErrorCode.POLICY_EXTERNAL_RESPONSE_PARSE_ERROR);
    }

    @ExceptionHandler(YouthCenterApiException.class)
    ResponseEntity<ApiResponse<Void>> handleYouthCenterApi(YouthCenterApiException exception) {
        return error(ErrorCode.POLICY_EXTERNAL_API_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.name(), errorCode.getMessage()));
    }
}
