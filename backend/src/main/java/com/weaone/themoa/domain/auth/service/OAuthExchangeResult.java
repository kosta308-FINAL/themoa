package com.weaone.themoa.domain.auth.service;

/**
 * 소셜(카카오·구글) 콜백 처리 직후의 분기 결과. 기존 회원이면 {@link #tokens}가, 신규 회원이면
 * {@link #signupTicket}·{@link #nickname}·{@link #email}이 채워진다(auth.md §6-1·§6-2).
 * {@link #email}은 구글처럼 provider가 이미 검증한 이메일이 있을 때만 채워진다.
 */
public record OAuthExchangeResult(
        boolean requiresSignup,
        IssuedTokens tokens,
        String signupTicket,
        String nickname,
        String email
) {

    public static OAuthExchangeResult loggedIn(IssuedTokens tokens) {
        return new OAuthExchangeResult(false, tokens, null, null, null);
    }

    public static OAuthExchangeResult requiresSignup(String signupTicket, String nickname, String email) {
        return new OAuthExchangeResult(true, null, signupTicket, nickname, email);
    }
}
