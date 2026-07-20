package com.weaone.themoa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ExchangeRateClientConfig {

    private final ExchangeRateProperties exchangeRateProperties;

    @Bean
    RestClient exchangeRateRestClient() {
        int timeoutMillis = (int) exchangeRateProperties.callTimeout().toMillis();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return RestClient.builder()
                .baseUrl(exchangeRateProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
