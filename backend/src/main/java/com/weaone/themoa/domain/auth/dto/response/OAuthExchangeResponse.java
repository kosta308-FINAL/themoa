package com.weaone.themoa.domain.auth.dto.response;

/**
 * 기존 회원이면 {@code token}이, 신규 회원이면 {@code signupTicket}·{@code nickname}이 채워진다.
 * 신규 회원의 경우 이 응답만으로는 로그인되지 않는다 — 추가정보 제출(complete-signup)까지 마쳐야 한다.
 * {@code email}은 구글처럼 provider가 이미 검증한 이메일이 있을 때만 채워지며, 프론트는 이 값이
 * 있으면 이메일 입력·인증 단계를 건너뛰고 읽기 전용으로 보여준다.
 */
public record OAuthExchangeResponse(
        boolean requiresSignup,
        TokenResponse token,
        String signupTicket,
        String nickname,
        String email
) {

    public static OAuthExchangeResponse loggedIn(TokenResponse token) {
        return new OAuthExchangeResponse(false, token, null, null, null);
    }

    public static OAuthExchangeResponse requiresSignup(String signupTicket, String nickname, String email) {
        return new OAuthExchangeResponse(true, null, signupTicket, nickname, email);
    }
}
