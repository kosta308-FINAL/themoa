package com.weaone.themoa.common.response;

public class ErrorResponse extends ApiResponse<Void> {

    private final String code;
    private final String message;

    ErrorResponse(String code, String message) {
        super(false);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}