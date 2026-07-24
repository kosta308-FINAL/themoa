package com.weaone.themoa.domain.auth.dto.response;

/**
 * 기존 회원이면 {@code token}이, 신규 회원이면 {@code signupTicket}·{@code nickname}이 채워진다.
 * 신규 회원의 경우 이 응답만으로는 로그인되지 않는다 — 추가정보 제출(complete-signup)까지 마쳐야 한다.
 */
public record OAuthExchangeResponse(
        boolean requiresSignup,
        TokenResponse token,
        String signupTicket,
        String nickname
) {

    public static OAuthExchangeResponse loggedIn(TokenResponse token) {
        return new OAuthExchangeResponse(false, token, null, null);
    }

    public static OAuthExchangeResponse requiresSignup(String signupTicket, String nickname) {
        return new OAuthExchangeResponse(true, null, signupTicket, nickname);
    }
}
