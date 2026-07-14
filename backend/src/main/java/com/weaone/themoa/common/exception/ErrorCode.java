package com.weaone.themoa.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 프론트엔드는 한글 메시지가 아니라 {@link #name()} 문자열 코드로 분기한다.
 * 코드 문자열은 API 계약이므로 한 번 공개된 값은 바꾸지 않는다.
 */
@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다."),

    // 인증 - 로그인/토큰
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."),

    // 인증 - 회원가입
    AUTH_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    AUTH_PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 서로 일치하지 않습니다."),
    AUTH_UNDERAGE(HttpStatus.BAD_REQUEST, "만 19세 이상만 가입할 수 있습니다."),

    // 인증 - 이메일 코드 인증
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증을 먼저 완료해 주세요."),
    AUTH_VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료되었습니다."),
    AUTH_VERIFICATION_RESEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 요청해 주세요."),
    AUTH_EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요."),

    // 카드 연동 - 커넥션
    CARD_ISSUER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 카드사입니다."),
    CARD_CONNECTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 연결된 카드사입니다."),
    CARD_CONNECTION_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요."),
    CARD_CONNECTION_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "카드사 로그인 정보가 올바르지 않습니다."),
    CARD_CONNECTION_LOCKED(HttpStatus.LOCKED, "카드사에서 계정이 잠겼습니다. 카드사 앱이나 고객센터에서 잠금을 해제한 후 다시 시도해 주세요."),
    CARD_CONNECTION_EXTERNAL_ERROR(HttpStatus.BAD_GATEWAY, "카드사 연결 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    // 가맹점 신원
    MERCHANT_ALIAS_NOT_FOUND(HttpStatus.NOT_FOUND, "가맹점 별칭을 찾을 수 없습니다."),
    MERCHANT_ALIAS_TERM_BILLER_FORBIDDEN(HttpStatus.BAD_REQUEST, "결제대행사 이름은 가맹점 표기로 등록할 수 없습니다."),
    MERCHANT_ALIAS_TERM_CONFLICT(HttpStatus.CONFLICT, "이미 다른 서비스로 등록된 표기입니다."),

    // 카드 거래
    CARD_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 내역을 찾을 수 없습니다."),
    CARD_TRANSACTION_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "카드로 수집된 거래는 삭제할 수 없습니다."),
    CARD_TRANSACTION_CANCEL_AMOUNT_NOT_CORRECTABLE(HttpStatus.CONFLICT, "취소금액을 정정할 수 있는 거래가 아닙니다."),
    CARD_TRANSACTION_AMOUNT_NOT_CORRECTABLE(HttpStatus.CONFLICT, "환산금액을 정정할 수 있는 거래가 아닙니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),

    // 카드 동기화
    CARD_SYNC_RECOVERY_NOT_ELIGIBLE(HttpStatus.CONFLICT, "복귀 동기화 대상이 아닙니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}