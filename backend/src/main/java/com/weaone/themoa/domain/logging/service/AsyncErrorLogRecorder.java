package com.weaone.themoa.domain.logging.service;

import com.weaone.themoa.domain.logging.dto.UnexpectedErrorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * 요청 스레드와 분리된 실행기로 {@code error_log} 저장을 요청한다(managelogging.md §3-4). 실행기 포화나
 * 저장 실패가 사용자 500 응답 생성을 방해하지 않도록, 모든 예외를 이 안에서 흡수하고 파일 로그로만 남긴다.
 */
@Slf4j
@Component
public class AsyncErrorLogRecorder {

    private final TaskExecutor errorLogTaskExecutor;
    private final ErrorLogPersistenceService errorLogPersistenceService;

    public AsyncErrorLogRecorder(@Qualifier("errorLogTaskExecutor") TaskExecutor errorLogTaskExecutor,
                                  ErrorLogPersistenceService errorLogPersistenceService) {
        this.errorLogTaskExecutor = errorLogTaskExecutor;
        this.errorLogPersistenceService = errorLogPersistenceService;
    }

    public void record(UnexpectedErrorEvent event) {
        try {
            errorLogTaskExecutor.execute(() -> save(event));
        } catch (TaskRejectedException e) {
            log.error("error_log 저장 작업이 거부되어 폐기합니다. traceId={}", event.traceId(), e);
        }
    }

    private void save(UnexpectedErrorEvent event) {
        try {
            errorLogPersistenceService.save(event);
        } catch (RuntimeException e) {
            log.error("error_log 저장에 실패했습니다. traceId={}", event.traceId(), e);
        }
    }
}
