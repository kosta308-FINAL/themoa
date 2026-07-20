package com.weaone.themoa.domain.recommend.finlife;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * finlife 호출용 RestClient(HTTP 클라이언트) 준비.
 * baseUrl을 미리 박아두어, 실제 호출 시엔 뒷부분 경로/파라미터만 붙이면 된다.
 * @EnableConfigurationProperties로 FinlifeProperties를 스프링 빈으로 등록한다.
 */
@Configuration
@EnableConfigurationProperties(FinlifeProperties.class)
public class FinlifeClientConfig {

    @Bean
    public RestClient finlifeRestClient(FinlifeProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())   // https://finlife.fss.or.kr/finlifeapi
                .build();
    }
}
