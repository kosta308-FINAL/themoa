package com.weaone.themoa.security.jwt;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    /** 발급 시점의 member.token_version 스냅샷. 매 요청 현재 값과 대조해 강제 무효화를 감지한다. */
    private static final String CLAIM_TOKEN_VERSION = "ver";

    /** 발급 시점의 member.role 스냅샷. /api/admin/** 인가에 사용한다(customerservice.md §3-2). */
    private static final String CLAIM_ROLE = "role";

    /** HMAC-SHA256 최소 키 길이. */
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey secretKey;
    private final Duration accessTokenValidity;

    public JwtTokenProvider(AuthProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.jwt().secret());
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT 서명키는 Base64 디코딩 후 최소 " + MIN_KEY_BYTES + "바이트여야 합니다.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.accessTokenValidity = properties.jwt().accessTokenValidity();
    }

    public String createAccessToken(Long memberId, int tokenVersion, String role, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(accessTokenValidity);
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 서명·만료·형식을 검증하고 클레임을 반환한다. 실패 원인(만료/위조/형식)은 응답으로 구분하지 않는다.
     */
    public AccessTokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AccessTokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_TOKEN_VERSION, Integer.class),
                    claims.get(CLAIM_ROLE, String.class)
            );
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
        }
    }

    public Duration getAccessTokenValidity() {
        return accessTokenValidity;
    }
}