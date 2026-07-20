package com.weaone.themoa.domain.policy.common.config;

import com.weaone.themoa.domain.policy.youthcenter.config.YouthCenterApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfig {
    @Bean
    HttpClient youthCenterHttpClient(YouthCenterApiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
