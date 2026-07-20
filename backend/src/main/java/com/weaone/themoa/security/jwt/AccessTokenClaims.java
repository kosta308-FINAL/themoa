package com.weaone.themoa.security.jwt;

/**
 * Access Token에서 꺼낸 값. 개인정보는 토큰에 담지 않으므로 회원 식별자·토큰 버전·역할뿐이다.
 */
public record AccessTokenClaims(Long memberId, int tokenVersion, String role) {
}