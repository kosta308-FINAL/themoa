package com.weaone.themoa.domain.recommend.finlife;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.properties의 finlife.* 설정값을 담는 클래스.
 * - finlife.base-url  → baseUrl
 * - finlife.api-key   → apiKey
 * (스프링이 케밥케이스 base-url ↔ 카멜케이스 baseUrl 자동 매칭)
 */
@ConfigurationProperties(prefix = "finlife")
public record FinlifeProperties(String baseUrl, String apiKey) {
}
