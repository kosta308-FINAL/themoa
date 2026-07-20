package com.weaone.themoa.domain.policy.youthcenter.client;

import com.weaone.themoa.common.exception.YouthCenterApiException;
import com.weaone.themoa.domain.policy.youthcenter.config.YouthCenterApiProperties;
import com.weaone.themoa.domain.policy.youthcenter.dto.request.YouthPolicyListRequest;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouthCenterApiClientConfigurationTest {
    @Test
    void currentApiCallFailsClearlyWhenApiKeyIsMissing() {
        YouthCenterApiClient client = new YouthCenterApiClient(HttpClient.newHttpClient(), new YouthCenterApiProperties());

        assertThatThrownBy(() -> client.fetchCurrentList(new YouthPolicyListRequest(null, null, null, null, null)))
                .isInstanceOf(YouthCenterApiException.class)
                .hasMessageContaining("온통청년 API Key가 설정되지 않았습니다.")
                .hasMessageContaining("config/application-secret.yml의 YOUTH_CENTER_API_KEY를 입력하세요.");
    }
}
