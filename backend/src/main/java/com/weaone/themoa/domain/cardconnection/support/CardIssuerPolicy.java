package com.weaone.themoa.domain.cardconnection.support;

/**
 * 카드사별 연결 입력 분기 규칙(connection.md §3-1). 현대카드만 ID 로그인 파라미터상 카드번호·카드비밀번호가
 * 필수다. {@link com.weaone.themoa.domain.cardconnection.service.CardConnectionService}의 서버측 검증과
 * {@code GET /api/card-issuers}의 화면 입력 필드 분기가 같은 기준을 공유하도록 여기 한 곳에 둔다.
 */
public final class CardIssuerPolicy {

    public static final String ORGANIZATION_HYUNDAI = "0302";

    private CardIssuerPolicy() {
    }

    public static boolean requiresCardCredentials(String organization) {
        return ORGANIZATION_HYUNDAI.equals(organization);
    }
}
