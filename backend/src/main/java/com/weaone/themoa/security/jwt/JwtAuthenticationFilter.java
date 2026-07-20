package com.weaone.themoa.security.jwt;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.security.handler.SecurityErrorResponder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization 헤더의 Access Token을 검증해 인증된 memberId를 SecurityContext에 넣는다.
 * 토큰이 없으면 통과시키고(공개 경로일 수 있다) 보호 자원 접근 시 EntryPoint가 401을 낸다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenVersionCache tokenVersionCache;
    private final SecurityErrorResponder errorResponder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AccessTokenClaims claims = jwtTokenProvider.parse(token);
            int currentTokenVersion = tokenVersionCache.find(claims.memberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN));
            if (currentTokenVersion != claims.tokenVersion()) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
            }
            SecurityContextHolder.getContext().setAuthentication(authentication(claims.memberId(), request));
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            errorResponder.respond(response, e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken authentication(Long memberId, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}