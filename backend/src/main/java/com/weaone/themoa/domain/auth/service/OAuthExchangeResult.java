package com.weaone.themoa.domain.auth.service;

/**
 * 카카오 콜백 처리 직후의 분기 결과. 기존 회원이면 {@link #tokens}가, 신규 회원이면
 * {@link #signupTicket}·{@link #nickname}이 채워진다(auth.md §6-1·§6-2).
 */
public record OAuthExchangeResult(
        boolean requiresSignup,
        IssuedTokens tokens,
        String signupTicket,
        String nickname
) {

    public static OAuthExchangeResult loggedIn(IssuedTokens tokens) {
        return new OAuthExchangeResult(false, tokens, null, null);
    }

    public static OAuthExchangeResult requiresSignup(String signupTicket, String nickname) {
        return new OAuthExchangeResult(true, null, signupTicket, nickname);
    }
}
