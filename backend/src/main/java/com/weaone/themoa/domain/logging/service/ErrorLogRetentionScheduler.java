package com.weaone.themoa.domain.logging.service;

import com.weaone.themoa.domain.logging.config.ManagementLoggingProperties;
import com.weaone.themoa.domain.logging.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 90일이 지난 {@code error_log}를 청크 삭제한다(managelogging.md §7-2). {@code ai_log_diagnosis}는
 * FK {@code ON DELETE CASCADE}로 함께 삭제된다. 이 클래스는 의도적으로 {@code @Transactional}을 걸지
 * 않는다 — {@link ErrorLogRepository}의 각 조회·삭제 호출이 Spring Data JPA 자체 트랜잭션으로 청크마다
 * 독립적으로 커밋되어야, 한 번의 거대한 DELETE 트랜잭션이 되는 걸 막을 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorLogRetentionScheduler {

    private final ErrorLogRepository errorLogRepository;
    private final ManagementLoggingProperties properties;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void purgeExpiredErrorLogs() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(properties.retentionDays());
        int deletedTotal = 0;
        for (int i = 0; i < properties.maxChunksPerRun(); i++) {
            List<Long> ids = errorLogRepository.findIdsCreatedBefore(threshold, PageRequest.of(0, properties.chunkSize()));
            if (ids.isEmpty()) {
                break;
            }
            errorLogRepository.deleteAllByIdInBatch(ids);
            deletedTotal += ids.size();
            if (ids.size() < properties.chunkSize()) {
                break;
            }
        }
        log.info("error_log 보관 정책 삭제를 완료했습니다. deletedTotal={}", deletedTotal);

        long remaining = errorLogRepository.countByCreatedAtBefore(threshold);
        if (remaining > 0) {
            log.warn("error_log 삭제 대상이 남아 있어 다음 실행에서 이어서 삭제합니다. remaining={}", remaining);
        }
    }
}
