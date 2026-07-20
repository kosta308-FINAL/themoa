package com.weaone.themoa.domain.policy.youthcenter.service;

import com.weaone.themoa.domain.policy.common.exception.YouthCenterApiException;
import com.weaone.themoa.domain.policy.youthcenter.client.ExternalApiResponse;
import com.weaone.themoa.domain.policy.youthcenter.client.YouthCenterApiClient;
import com.weaone.themoa.domain.policy.youthcenter.config.YouthCenterApiProperties;
import com.weaone.themoa.domain.policy.youthcenter.dto.parsed.ParsedPolicyList;
import com.weaone.themoa.domain.policy.youthcenter.dto.request.YouthPolicyListRequest;
import com.weaone.themoa.domain.policy.youthcenter.dto.response.YouthCenterProbeResponse;
import com.weaone.themoa.domain.policy.youthcenter.dto.response.YouthCenterStatusResponse;
import com.weaone.themoa.domain.policy.youthcenter.mapper.YouthPolicyMapper;
import com.weaone.themoa.domain.policy.youthcenter.parser.ResponseType;
import com.weaone.themoa.domain.policy.youthcenter.parser.YouthCenterResponseParser;
import com.weaone.themoa.domain.policy.youthcenter.util.RawResponseStorage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class YouthCenterDiagnosticService {
    private final YouthCenterApiProperties properties;
    private final YouthCenterApiClient client;
    private final YouthCenterResponseParser parser;
    private final RawResponseStorage storage;
    private final Environment environment;
    private final YouthPolicyMapper mapper;

    public YouthCenterDiagnosticService(YouthCenterApiProperties properties, YouthCenterApiClient client,
                                        YouthCenterResponseParser parser, RawResponseStorage storage,
                                        Environment environment, YouthPolicyMapper mapper) {
        this.properties = properties;
        this.client = client;
        this.parser = parser;
        this.storage = storage;
        this.environment = environment;
        this.mapper = mapper;
    }

    public YouthCenterStatusResponse status() {
        return new YouthCenterStatusResponse("UP", properties.getApiMode().name(),
                properties.getCurrent().getApiKey() != null && !properties.getCurrent().getApiKey().isBlank(),
                properties.getCurrent().getBaseUrl(), properties.getCurrent().getPath(),
                properties.isFollowRedirects(), properties.getRawResponse().isSaveEnabled());
    }

    public YouthCenterProbeResponse probe() {
        YouthPolicyListRequest request = new YouthPolicyListRequest(properties.getDefaultPageNumber(),
                properties.getDefaultPageSize(), properties.getDefaultReturnType(), null, null);
        ExternalApiResponse response = client.fetchCurrentList(request);
        return buildProbe("CURRENT", response, requestParams(request));
    }

    public YouthCenterProbeResponse legacyProbe() {
        if (!isLocalProfile()) {
            throw new YouthCenterApiException("援щ쾭??API 吏꾨떒? local ?꾨줈?꾩뿉?쒕쭔 ?ъ슜?????덉뒿?덈떎.");
        }
        ExternalApiResponse response = client.fetchLegacyProbe();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("openApiVlak", "****");
        params.put("pageIndex", 1);
        params.put("display", 10);
        return buildProbe("LEGACY_DIAGNOSTIC", response, params);
    }

    private YouthCenterProbeResponse buildProbe(String mode, ExternalApiResponse response, Map<String, Object> requestParams) {
        ResponseType type = parser.detect(response);
        ParsedPolicyList parsed = null;
        boolean error = false;
        String errorMessage = null;
        try {
            parsed = parser.parseList(response);
        } catch (RuntimeException ex) {
            error = true;
            errorMessage = ex.getMessage();
        }
        String filePath = storage.save(mode.toLowerCase() + "-probe", type, response.body());
        return new YouthCenterProbeResponse(mode,
                properties.getCurrent().getApiKey() != null && !properties.getCurrent().getApiKey().isBlank(),
                response.maskedRequestUrl(), requestParams, response.statusCode(), response.contentType(), type,
                response.redirected(), response.redirectLocation(), response.redirectHistory(), response.finalMaskedUrl(),
                response.warnings(), response.elapsedTimeMs(), response.bodyLength(), preview(response.body()), filePath,
                error, null, errorMessage, parsed != null && parsed.listNodeFound(), parsed == null ? null : parsed.listNodePath(),
                parsed == null ? 0 : parsed.policies().size(), parsed == null ? null : parsed.totalCount(),
                parsed == null ? null : parsed.currentPage(), parsed == null ? null : parsed.pageSize(),
                parsed == null || parsed.policies().isEmpty() ? null : mapper.toView(parsed.policies().get(0)),
                parsed == null ? null : parsed.schemaAnalysis());
    }

    private Map<String, Object> requestParams(YouthPolicyListRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("apiKeyNm", "****");
        params.put("pageNum", request.effectivePageNum(properties));
        params.put("pageSize", request.effectivePageSize(properties));
        params.put("pageType", 1);
        params.put("rtnType", request.effectiveReturnType(properties));
        return params;
    }

    private boolean isLocalProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("local".equals(profile)) {
                return true;
            }
        }
        return environment.getActiveProfiles().length == 0;
    }

    private String preview(String body) {
        if (body == null) {
            return "";
        }
        int length = Math.min(body.length(), properties.getRawResponse().getPreviewLength());
        return body.substring(0, length);
    }
}
