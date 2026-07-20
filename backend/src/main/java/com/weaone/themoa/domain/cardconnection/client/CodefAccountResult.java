package com.weaone.themoa.domain.cardconnection.client;

/**
 * CODEF 계정 등록 응답을 우리 도메인 모델로 옮긴 결과. CODEF 원문 JSON을 서비스 계층까지 흘리지 않는다.
 *
 * @param userErrorCode 카드사 계정 잠금 임박/발생 신호(connection.md §5-2). 체크하지 않는 카드사는 빈 값으로 온다.
 */
public record CodefAccountResult(
        boolean success,
        String connectedId,
        String resultCode,
        String resultMessage,
        String userErrorCode
) {

    public static CodefAccountResult success(String connectedId, String resultCode, String resultMessage) {
        return new CodefAccountResult(true, connectedId, resultCode, resultMessage, "");
    }

    public static CodefAccountResult failure(String resultCode, String resultMessage, String userErrorCode) {
        return new CodefAccountResult(false, null, resultCode, resultMessage, userErrorCode);
    }
}
