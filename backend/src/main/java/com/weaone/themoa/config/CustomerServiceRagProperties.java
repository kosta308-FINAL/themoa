package com.weaone.themoa.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.customer-service.rag")
public record CustomerServiceRagProperties(
        boolean enabled,
        boolean embedOnStartup,
        @NotBlank String collectionName,
        @Min(1) int topK,
        double minimumSimilarity
) {
}
