package com.weaone.themoa.common.exception;

import lombok.Getter;

/**
 * 예상 가능한 비즈니스 실패. 트랜잭션 롤백이 걸리도록 unchecked로 둔다.
 * 메시지는 {@link ErrorCode}의 기본 메시지만 사용한다. 사용자 입력·민감정보를 메시지에 담지 않는다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}