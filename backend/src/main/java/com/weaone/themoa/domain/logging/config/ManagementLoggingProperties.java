package com.weaone.themoa.domain.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 에러 로그 보관 정책 설정(managelogging.md §7-2). 설정이 없어도 기본값(90일, 1,000건, 100회)으로
 * 동작해야 한다.
 */
@ConfigurationProperties(prefix = "app.management-logging")
public record ManagementLoggingProperties(
        @DefaultValue("90") int retentionDays,
        @DefaultValue("1000") int chunkSize,
        @DefaultValue("100") int maxChunksPerRun
) {
}
