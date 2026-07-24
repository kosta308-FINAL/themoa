package com.weaone.themoa.domain.policy.sync.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.domain.datarefresh.entity.DataRefreshSource;
import com.weaone.themoa.domain.datarefresh.service.DataRefreshStatusService;
import com.weaone.themoa.domain.policy.policy.service.PolicyCollectionExecutionType;
import com.weaone.themoa.domain.policy.recommendation.service.PolicyRecommendationBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(prefix = "app.policy.sync", name = "enabled", havingValue = "true")
public class PolicyDailySyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(PolicyDailySyncScheduler.class);
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final PolicySyncExecutionGuard executionGuard;
    private final PolicySyncPipelineService pipelineService;
    private final TaskExecutor adminJobExecutor;
    private final DataRefreshStatusService dataRefreshStatusService;
    private final PolicyRecommendationBatchService recommendationBatchService;

    public PolicyDailySyncScheduler(PolicySyncExecutionGuard executionGuard,
                                    PolicySyncPipelineService pipelineService,
                                    @Qualifier("adminJobExecutor") TaskExecutor adminJobExecutor,
                                    DataRefreshStatusService dataRefreshStatusService,
                                    PolicyRecommendationBatchService recommendationBatchService) {
        this.executionGuard = executionGuard;
        this.pipelineService = pipelineService;
        this.adminJobExecutor = adminJobExecutor;
        this.dataRefreshStatusService = dataRefreshStatusService;
        this.recommendationBatchService = recommendationBatchService;
    }

    @Scheduled(
            cron = "${app.policy.sync.cron:0 0 4 * * *}",
            zone = "${app.policy.sync.zone:Asia/Seoul}"
    )
    public void scheduleDailySync() {
        if (!executionGuard.tryAcquire()) {
            log.info("다른 정책 작업이 실행 중이므로 일일 동기화를 건너뜁니다.");
            return;
        }
        try {
            adminJobExecutor.execute(this::runDailySync);
        } catch (RuntimeException ex) {
            executionGuard.release();
            log.warn("정책 일일 동기화 작업 제출에 실패했습니다. errorType={}",
                    ex.getClass().getSimpleName());
        }
    }

    private void runDailySync() {
        try {
            log.info("정책 일일 동기화를 시작합니다.");
            PolicySyncPipelineResult result = pipelineService.synchronize(
                    PolicySyncMode.INCREMENTAL,
                    PolicyCollectionExecutionType.SCHEDULED,
                    null
            );
            log.info("정책 일일 동기화를 완료했습니다. collected={}, queuedEmbeddings={}, embeddingSuccess={}, embeddingFailed={}, pending={}, ready={}",
                    result.collection().receivedCount(),
                    result.embeddingQueue().newlyQueuedCount() + result.embeddingQueue().requeuedCount(),
                    result.embeddingProcess().successCount(),
                    result.embeddingProcess().failedCount(),
                    result.embeddingProcess().pendingCountAfter(),
                    result.readiness().ready());
            if (result.readiness().ready()) {
                dataRefreshStatusService.recordSuccess(DataRefreshSource.POLICY, LocalDateTime.now(SEOUL_ZONE));
                refreshPolicyRecommendations();
            }
        } catch (BusinessException ex) {
            log.warn("정책 일일 동기화에 실패했습니다. errorCode={}",
                    ex.getErrorCode().name());
        } catch (RuntimeException ex) {
            log.warn("정책 일일 동기화에 실패했습니다. errorType={}",
                    ex.getClass().getSimpleName());
        } finally {
            executionGuard.release();
        }
    }

    private void refreshPolicyRecommendations() {
        try {
            recommendationBatchService.refreshAllProfiles();
        } catch (RuntimeException ex) {
            log.warn("정책 추천 Batch 실행에 실패했습니다. errorType={}", ex.getClass().getSimpleName());
        }
    }
}
