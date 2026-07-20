package com.weaone.themoa.domain.policy.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.policy.sync")
public record PolicySyncProperties(
        boolean enabled,
        String cron,
        String zone
) {
    public static final String DEFAULT_CRON = "0 0 4 * * *";
    public static final String DEFAULT_ZONE = "Asia/Seoul";

    public PolicySyncProperties {
        cron = hasText(cron) ? cron : DEFAULT_CRON;
        zone = hasText(zone) ? zone : DEFAULT_ZONE;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
