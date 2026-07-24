package com.weaone.themoa.security.oauth;

import com.weaone.themoa.config.AuthProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오 콜백 처리 후 프론트로 돌려보낼 URL을 만든다. 배포는 nginx가 동일 origin에서 프론트를
 * 서빙하므로 상대 경로만으로 충분하고(distribution/distributionSetting.md §10.2), 로컬은 프론트(Vite,
 * 5173)와 백엔드(8080) 포트가 달라 {@code app.auth.oauth.frontend-redirect-base-url}로 절대경로를 쓴다.
 */
@Component
public class OAuthFrontendRedirectResolver {

    private final String baseUrl;

    public OAuthFrontendRedirectResolver(AuthProperties properties) {
        this.baseUrl = properties.oauth().frontendRedirectBaseUrl();
    }

    /** @param path {@code /}로 시작하는 프론트 라우트 경로. */
    public String resolve(String path, String queryParamName, String queryParamValue) {
        UriComponentsBuilder builder = (baseUrl == null || baseUrl.isBlank())
                ? UriComponentsBuilder.fromPath(path)
                : UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        return builder.queryParam(queryParamName, queryParamValue).build().toUriString();
    }
}
