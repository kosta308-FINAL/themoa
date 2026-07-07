package com.weaone.themoa.common.response;

/**
 * 모든 API 응답의 공통 부모.
 * 실제 응답은 항상 {@link SuccessResponse} 또는 {@link ErrorResponse}로만 존재한다.
 */
public abstract class ApiResponse<T> {

    private final boolean success;

    protected ApiResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new SuccessResponse<>(data);
    }

    public static ApiResponse<Void> success() {
        return new SuccessResponse<>(null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ErrorResponse(code, message);
    }
}