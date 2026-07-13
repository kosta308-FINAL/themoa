package com.weaone.themoa.domain.auth.service;

import java.time.Duration;

/**
 * 서비스가 발급한 토큰 한 쌍. Refresh Token 원문은 여기서만 오가며 쿠키로 나가고, 저장되지 않는다.
 */
public record IssuedTokens(
        String accessToken,
        Duration accessTokenValidity,
        String refreshToken,
        Duration refreshTokenValidity
) {
}