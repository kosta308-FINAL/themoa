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
    CARD_CONNECTION_BIRTHDATE_REQUIRED(HttpStatus.BAD_REQUEST, "본인 확인을 위해 생년월일(주민등록번호)을 입력한 뒤 다시 시도해 주세요."),
    CARD_CONNECTION_EXTERNAL_ERROR(HttpStatus.BAD_GATEWAY, "카드사 연결 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    CARD_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "연결된 카드사를 찾을 수 없습니다."),
    CARD_CONNECTION_RETRY_NOT_ALLOWED(HttpStatus.CONFLICT, "실패한 초기수집만 다시 시도할 수 있습니다."),

    // 가맹점 신원
    MERCHANT_ALIAS_NOT_FOUND(HttpStatus.NOT_FOUND, "가맹점 별칭을 찾을 수 없습니다."),
    MERCHANT_ALIAS_TERM_BILLER_FORBIDDEN(HttpStatus.BAD_REQUEST, "결제대행사 이름은 가맹점 표기로 등록할 수 없습니다."),
    MERCHANT_ALIAS_TERM_CONFLICT(HttpStatus.CONFLICT, "이미 다른 서비스로 등록된 표기입니다."),
    MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "가맹점을 찾을 수 없습니다."),

    // 카드 거래
    CARD_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 내역을 찾을 수 없습니다."),
    CARD_TRANSACTION_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "카드로 수집된 거래는 삭제할 수 없습니다."),
    CARD_TRANSACTION_CANCEL_AMOUNT_NOT_CORRECTABLE(HttpStatus.CONFLICT, "취소금액을 정정할 수 있는 거래가 아닙니다."),
    CARD_TRANSACTION_AMOUNT_NOT_CORRECTABLE(HttpStatus.CONFLICT, "환산금액을 정정할 수 있는 거래가 아닙니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),

    // 카드 동기화
    CARD_SYNC_RECOVERY_NOT_ELIGIBLE(HttpStatus.CONFLICT, "복귀 동기화 대상이 아닙니다."),

    // 입력 모드 · 수기 입력
    MANUAL_CARD_ENTRY_NOT_ALLOWED(HttpStatus.FORBIDDEN, "카드 자동수집이 켜져 있는 동안은 카드 결제를 직접 입력할 수 없습니다."),

    // 고정지출
    FIXED_EXPENSE_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "고정지출 추천 후보를 찾을 수 없습니다."),
    FIXED_EXPENSE_CANDIDATE_NOT_PENDING(HttpStatus.CONFLICT, "이미 응답한 추천 후보입니다."),
    FIXED_EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "고정지출을 찾을 수 없습니다."),
    FIXED_EXPENSE_MERCHANT_ALIAS_REQUIRED(HttpStatus.BAD_REQUEST, "카드형 고정지출은 가맹점을 선택해야 합니다."),
    FIXED_EXPENSE_EXCHANGE_RATE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "환율 정보를 구하지 못해 등록할 수 없습니다. 잠시 후 다시 시도해 주세요."),

    // 알림
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),

    // 습관성 지출 코칭
    COACHING_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "코칭 카드를 찾을 수 없습니다."),

    // 회원 · 소비 가이드 예산
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    SPENDING_GUIDE_SETUP_REQUIRED(HttpStatus.BAD_REQUEST, "월급과 급여일을 먼저 설정해 주세요."),
    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "예산 주기를 찾을 수 없습니다."),
    BUDGET_FUTURE_CYCLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "아직 시작하지 않은 급여 주기입니다."),
    INCOME_PROFILE_INVALID(HttpStatus.BAD_REQUEST, "소득유형에 맞는 소득 정보를 입력해 주세요."),
    WORK_SCHEDULE_EMPTY(HttpStatus.BAD_REQUEST, "요일별 근무시간을 하나 이상 입력해 주세요."),
    INCOME_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "현재 소득유형과 일치하지 않는 요청입니다."),
    INCOME_ADJUSTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수입 보정 내역을 찾을 수 없습니다."),
    INCOME_ADJUSTMENT_AMOUNT_ZERO(HttpStatus.BAD_REQUEST, "0원은 입력할 수 없습니다."),

    // Policy
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "정책을 찾을 수 없습니다."),
    POLICY_SEARCH_NOT_READY(HttpStatus.CONFLICT, "정책 검색 데이터가 준비되지 않았습니다."),
    POLICY_JOB_ALREADY_RUNNING(HttpStatus.CONFLICT, "이미 실행 중인 정책 작업이 있습니다."),
    POLICY_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "정책 관리 작업을 찾을 수 없습니다."),
    POLICY_ADMIN_OPERATION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 정책 관리 작업입니다."),
    POLICY_SEARCH_DIAGNOSTIC_NOT_READY(HttpStatus.CONFLICT, "정책 검색 진단 실행 조건이 충족되지 않았습니다."),
    POLICY_DIAGNOSTIC_DATA_INVALID(HttpStatus.BAD_REQUEST, "정책 검색 진단 데이터가 유효하지 않습니다."),
    POLICY_VECTOR_STORE_NOT_READY(HttpStatus.CONFLICT, "정책 벡터 저장소 또는 임베딩이 준비되지 않았습니다."),
    POLICY_DIAGNOSTIC_REPORT_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정책 검색 진단 리포트를 저장하지 못했습니다."),
    POLICY_DATA_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정책 데이터를 처리하지 못했습니다."),
    POLICY_COLLECTION_DISABLED(HttpStatus.CONFLICT, "정책 수집 기능이 비활성화되어 있습니다."),
    POLICY_EXTERNAL_API_KEY_REQUIRED(HttpStatus.CONFLICT, "정책 외부 API 키 설정이 필요합니다."),
    POLICY_EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "정책 외부 API 요청을 처리하지 못했습니다."),
    POLICY_EXTERNAL_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "정책 외부 API 응답을 해석하지 못했습니다."),
    POLICY_REGION_SYNC_CONFIG_REQUIRED(HttpStatus.CONFLICT, "SGIS 지역 동기화 설정이 필요합니다."),
    POLICY_REGION_CATALOG_NOT_READY(HttpStatus.CONFLICT, "정책 지역 카탈로그가 준비되지 않았습니다."),
    POLICY_REGION_EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "지역 외부 API 요청을 처리하지 못했습니다."),
    POLICY_REGION_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "지역 외부 API 응답을 해석하지 못했습니다."),
    POLICY_REGION_REBUILD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정책 지역 재분류를 완료하지 못했습니다."),
    POLICY_REGION_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정책 지역 동기화 작업을 완료하지 못했습니다."),
    POLICY_EMBEDDING_CONFIG_REQUIRED(HttpStatus.CONFLICT, "정책 임베딩 설정이 필요합니다."),
    POLICY_EMBEDDING_SYNC_NOT_FOUND(HttpStatus.CONFLICT, "정책 임베딩 동기화 정보를 찾을 수 없습니다."),
    POLICY_QDRANT_ERROR(HttpStatus.BAD_GATEWAY, "정책 벡터 저장소 요청을 처리하지 못했습니다."),

    // 고객센터 - FAQ
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FAQ를 찾을 수 없습니다."),

    // 고객센터 - 1:1 문의
    CUSTOMER_INQUIRY_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    CUSTOMER_INQUIRY_PRIVACY_REQUIRED(HttpStatus.BAD_REQUEST, "개인정보 수집·이용에 동의해 주세요."),
    CUSTOMER_INQUIRY_FILE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "첨부파일 개수 또는 용량이 허용 범위를 초과했습니다."),
    CUSTOMER_INQUIRY_FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 파일 형식입니다."),
    CUSTOMER_INQUIRY_ANSWER_CONFLICT(HttpStatus.CONFLICT, "이미 답변이 등록되었거나 화면이 최신 상태가 아닙니다. 새로고침 후 다시 시도해 주세요."),
    CUSTOMER_INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."),
    CUSTOMER_INQUIRY_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다."),
    CUSTOMER_INQUIRY_FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "첨부파일을 저장하지 못했습니다.");
    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
