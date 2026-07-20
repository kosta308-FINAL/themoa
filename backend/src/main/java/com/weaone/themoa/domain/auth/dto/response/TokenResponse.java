package com.weaone.themoa.domain.auth.dto.response;

import com.weaone.themoa.domain.auth.service.IssuedTokens;

/**
 * Access Token만 본문으로 내려간다. Refresh Token은 HttpOnly 쿠키로만 나가며 본문에 담지 않는다.
 *
 * @param expiresIn Access Token 만료까지 남은 초.
 */
public record TokenResponse(String accessToken, long expiresIn) {

    public static TokenResponse from(IssuedTokens tokens) {
        return new TokenResponse(tokens.accessToken(), tokens.accessTokenValidity().toSeconds());
    }
}