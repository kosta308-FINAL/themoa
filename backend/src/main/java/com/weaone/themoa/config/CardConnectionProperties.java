package com.weaone.themoa.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 카드사 로그인 실패 쿨다운 설정값(connection.md §5-1). 카드사 실제 계정 잠금(≈5회)보다 먼저 끊어
 * 사용자 계정이 카드사 쪽에서 잠기는 것을 예방한다.
 */
@Validated
@ConfigurationProperties(prefix = "app.card-connection")
public record CardConnectionProperties(
        @NotNull Duration loginFailCooldown,
        @Positive int maxLoginFailCount
) {
}
