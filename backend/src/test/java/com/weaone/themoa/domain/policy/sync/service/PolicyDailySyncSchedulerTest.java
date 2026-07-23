package com.weaone.themoa.domain.policy.sync.service;

import com.weaone.themoa.domain.datarefresh.service.DataRefreshStatusService;
import com.weaone.themoa.domain.policy.policy.service.PolicyCollectionExecutionType;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyDailySyncSchedulerTest {
    private final PolicySyncExecutionGuard executionGuard = mock(PolicySyncExecutionGuard.class);
    private final PolicySyncPipelineService pipelineService = mock(PolicySyncPipelineService.class);
    private final DataRefreshStatusService dataRefreshStatusService = mock(DataRefreshStatusService.class);

    @Test
    void successfulAcquireRunsIncrementalScheduledPipeline() {
        when(executionGuard.tryAcquire()).thenReturn(true);

        scheduler().scheduleDailySync();

        verify(pipelineService).synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.SCHEDULED, null);
        verify(executionGuard).release();
    }

    @Test
    void failedAcquireSkipsPipeline() {
        when(executionGuard.tryAcquire()).thenReturn(false);

        scheduler().scheduleDailySync();

        verify(pipelineService, never()).synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.SCHEDULED, null);
        verify(executionGuard, never()).release();
    }

    @Test
    void releaseGuardAfterNormalCompletion() {
        when(executionGuard.tryAcquire()).thenReturn(true);

        scheduler().scheduleDailySync();

        verify(executionGuard).release();
    }

    @Test
    void releaseGuardAfterPipelineException() {
        when(executionGuard.tryAcquire()).thenReturn(true);
        when(pipelineService.synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.SCHEDULED, null))
                .thenThrow(new IllegalStateException("failed"));

        scheduler().scheduleDailySync();

        verify(executionGuard).release();
    }

    @Test
    void pipelineExceptionDoesNotPropagateToSchedulerCaller() {
        when(executionGuard.tryAcquire()).thenReturn(true);
        when(pipelineService.synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.SCHEDULED, null))
                .thenThrow(new IllegalStateException("failed"));

        assertThatCode(() -> scheduler().scheduleDailySync()).doesNotThrowAnyException();
    }

    private PolicyDailySyncScheduler scheduler() {
        return new PolicyDailySyncScheduler(
                executionGuard,
                pipelineService,
                new SyncTaskExecutor(),
                dataRefreshStatusService
        );
    }
}
