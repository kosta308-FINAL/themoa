package com.weaone.themoa.domain.policy.common.exception;

public class YouthCenterApiException extends RuntimeException {
    public YouthCenterApiException(String message) {
        super(message);
    }

    public YouthCenterApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
