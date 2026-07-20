package com.weaone.themoa.config;

import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefServiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class CodefConfig {

    private final CodefProperties codefProperties;

    @Bean
    EasyCodef easyCodef() {
        EasyCodef codef = new EasyCodef();
        if (codefProperties.serviceType() == EasyCodefServiceType.DEMO) {
            codef.setClientInfoForDemo(codefProperties.clientId(), codefProperties.clientSecret());
        } else {
            codef.setClientInfo(codefProperties.clientId(), codefProperties.clientSecret());
        }
        codef.setPublicKey(codefProperties.publicKey());
        return codef;
    }

    /** EasyCodef SDK가 커넥션/응답 타임아웃을 노출하지 않아, 블로킹 호출을 이 executor에서 실행해 직접 데드라인을 강제한다. */
    @Bean(destroyMethod = "shutdown")
    ExecutorService codefExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
