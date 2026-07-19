package com.weaone.themoa.domain.policy.youthcenter.dto.response;

public record YouthCenterStatusResponse(
        String application,
        String apiMode,
        boolean apiKeyConfigured,
        String baseUrl,
        String path,
        boolean followRedirects,
        boolean rawResponseSaveEnabled
) {
}
