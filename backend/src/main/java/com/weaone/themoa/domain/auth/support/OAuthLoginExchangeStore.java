package com.weaone.themoa.domain.auth.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.weaone.themoa.config.AuthProperties;
import com.weaone.themoa.domain.auth.service.OAuthExchangeResult;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 카카오 콜백(브라우저 최상위 redirect) → 프론트 사이를 잇는 1회용 교환코드 저장소(auth.md §8).
 * Access/Refresh Token 원문을 콜백 URL의 query parameter에 싣지 않기 위한 중계 용도이며,
 * 프론트가 {@code POST /api/auth/oauth/exchange}로 1회 소비하면 즉시 폐기된다.
 */
@Component
public class OAuthLoginExchangeStore {

    private final Cache<String, OAuthExchangeResult> results;
    private final RefreshTokenGenerator codeGenerator;

    public OAuthLoginExchangeStore(AuthProperties properties, RefreshTokenGenerator codeGenerator) {
        this.results = Caffeine.newBuilder()
                .expireAfterWrite(properties.oauth().exchangeCodeTtl())
                .build();
        this.codeGenerator = codeGenerator;
    }

    public String issue(OAuthExchangeResult result) {
        String code = codeGenerator.generate();
        results.put(code, result);
        return code;
    }

    public Optional<OAuthExchangeResult> consume(String code) {
        OAuthExchangeResult result = results.getIfPresent(code);
        if (result != null) {
            results.invalidate(code);
        }
        return Optional.ofNullable(result);
    }
}
