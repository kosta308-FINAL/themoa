package com.weaone.themoa.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 한국수출입은행 환율 API 설정값(cardtransaction.md §4 FX-03). 인증키는 소스에 두지 않고 환경변수로 주입한다.
 */
@Validated
@ConfigurationProperties(prefix = "app.exchange-rate")
public record ExchangeRateProperties(
        @NotBlank String authKey,
        @NotBlank String baseUrl,
        @NotNull Duration callTimeout
) {
}
