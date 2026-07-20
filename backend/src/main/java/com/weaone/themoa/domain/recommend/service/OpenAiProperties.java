package com.weaone.themoa.domain.recommend.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI 설정. openai.api-key / openai.model.
 * 키가 비어 있으면 LLM 최종선택을 건너뛰고 규칙 기반 결과를 쓴다.
 */
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey, String model) {

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
