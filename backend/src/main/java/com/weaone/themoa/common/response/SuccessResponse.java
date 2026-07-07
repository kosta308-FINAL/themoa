package com.weaone.themoa.common.response;

public class SuccessResponse<T> extends ApiResponse<T> {

    private final T data;

    SuccessResponse(T data) {
        super(true);
        this.data = data;
    }

    public T getData() {
        return data;
    }
}