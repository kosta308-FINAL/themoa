package com.weaone.themoa.domain.logging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 에러 로그 저장·AI 진단 전용 실행기(managelogging.md §3-4, §6-4). 요청 스레드와 분리한다. */
@Configuration
public class ManagementLoggingAsyncConfig {

    @Bean("errorLogTaskExecutor")
    public TaskExecutor errorLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("error-log-");
        executor.initialize();
        return executor;
    }

    @Bean("aiDiagnosisTaskExecutor")
    public TaskExecutor aiDiagnosisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ai-diagnosis-");
        executor.initialize();
        return executor;
    }
}
