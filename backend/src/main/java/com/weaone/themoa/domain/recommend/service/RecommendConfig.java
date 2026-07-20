package com.weaone.themoa.domain.recommend.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 추천 모듈 설정 - openai.* 프로퍼티 등록. */
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class RecommendConfig {
}
