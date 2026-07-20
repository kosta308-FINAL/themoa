package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.service.PolicyCollectionExecutionType;
import com.weaone.themoa.domain.policy.policy.service.PolicyCollectionResult;
import com.weaone.themoa.domain.policy.policy.service.PolicyRegionRebuildResult;
import com.weaone.themoa.domain.policy.policy.service.PolicyRegionRebuildService;
import com.weaone.themoa.domain.policy.policy.service.YouthCenterPolicyCollectionService;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingProcessResult;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingQueueResult;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmbeddingService;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndex;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndexBuilder;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService;
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationService;
import com.weaone.themoa.domain.policy.sync.service.PolicySyncExecutionGuard;
import com.weaone.themoa.domain.policy.sync.service.PolicySyncMode;
import com.weaone.themoa.domain.policy.sync.service.PolicySyncPipelineResult;
import com.weaone.themoa.domain.policy.sync.service.PolicySyncPipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminJobServiceTest {
    private final YouthCenterPolicyCollectionService collectionService = mock(YouthCenterPolicyCollectionService.class);
    private final PolicyEmbeddingService embeddingService = mock(PolicyEmbeddingService.class);
    private final PolicyRegionRebuildService regionRebuildService = mock(PolicyRegionRebuildService.class);
    private final RegionSynchronizationService regionSynchronizationService = mock(RegionSynchronizationService.class);
    private final PolicySearchProjectionService projectionService = mock(PolicySearchProjectionService.class);
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder = mock(PolicyLexicalIndexBuilder.class);
    private final RegionCodeRepository regionCodeRepository = mock(RegionCodeRepository.class);
    private final PolicySyncPipelineService policySyncPipelineService = mock(PolicySyncPipelineService.class);
    private final PolicySyncExecutionGuard policySyncExecutionGuard = mock(PolicySyncExecutionGuard.class);

    @Test
    void searchProjectionRebuildRefreshesLexicalIndexAndCompletes() {
        allowJobStart();
        when(projectionService.rebuildAll(anyProjectionProgressConsumer()))
                .thenReturn(new PolicySearchProjectionService.ProjectionRebuildResult(2650, 2650, 0));
        PolicyLexicalIndex index = index(2650);
        when(lexicalIndexBuilder.refresh()).thenReturn(index);

        AdminJobStatus status = service().start("SEARCH_PROJECTION_REBUILD");

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.message()).contains("projectionCount=2650", "indexDocumentCount=2650");
        verify(projectionService).rebuildAll(anyProjectionProgressConsumer());
        verify(lexicalIndexBuilder).refresh();
        verify(policySyncExecutionGuard).release();
    }

    @Test
    void searchIndexRefreshBuildsIndexAndCompletes() {
        allowJobStart();
        PolicyLexicalIndex index = index(2650);
        when(lexicalIndexBuilder.refresh()).thenReturn(index);

        AdminJobStatus status = service().start("SEARCH_INDEX_REFRESH");

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.message()).contains("documentCount=2650");
        verify(lexicalIndexBuilder).refresh();
        verify(policySyncExecutionGuard).release();
    }

    @Test
    void policySyncRunsIncrementalManualPipeline() {
        allowJobStart();
        when(policySyncPipelineService.synchronize(eq(PolicySyncMode.INCREMENTAL), eq(PolicyCollectionExecutionType.MANUAL),
                anyJobProgressConsumer())).thenReturn(syncResult(0));

        AdminJobStatus status = service().start("POLICY_SYNC");

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.message()).contains("POLICY_SYNC_COMPLETED", "queuedEmbeddingCount=12");
        verify(policySyncPipelineService).synchronize(eq(PolicySyncMode.INCREMENTAL), eq(PolicyCollectionExecutionType.MANUAL),
                anyJobProgressConsumer());
        verify(policySyncExecutionGuard).release();
    }

    @Test
    void fullReindexRunsFullReindexManualPipeline() {
        allowJobStart();
        when(policySyncPipelineService.synchronize(eq(PolicySyncMode.FULL_REINDEX), eq(PolicyCollectionExecutionType.MANUAL),
                anyJobProgressConsumer())).thenReturn(syncResult(0));

        AdminJobStatus status = service().start("FULL_REINDEX");

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.message()).contains("FULL_REINDEX_COMPLETED");
        verify(policySyncPipelineService).synchronize(eq(PolicySyncMode.FULL_REINDEX), eq(PolicyCollectionExecutionType.MANUAL),
                anyJobProgressConsumer());
        verify(policySyncExecutionGuard).release();
    }

    @Test
    void failedPolicyCollectionDoesNotInvalidateLexicalIndex() {
        allowJobStart();
        when(collectionService.collectAll(anyJobProgressConsumer())).thenReturn(collectionResult("FAILED"));

        AdminJobStatus status = service().start("POLICY_COLLECTION");

        assertThat(status.status()).isEqualTo("FAILED");
        verify(collectionService).collectAll(anyJobProgressConsumer());
        verify(lexicalIndexBuilder, never()).invalidate();
        verify(policySyncExecutionGuard).release();
    }

    @Test
    void successfulPolicyCollectionInvalidatesLexicalIndex() {
        allowJobStart();
        when(collectionService.collectAll(anyJobProgressConsumer())).thenReturn(collectionResult("COMPLETED"));

        AdminJobStatus status = service().start("POLICY_COLLECTION");

        assertThat(status.status()).isEqualTo("COMPLETED");
        verify(collectionService).collectAll(anyJobProgressConsumer());
        verify(lexicalIndexBuilder).invalidate();
        verify(policySyncExecutionGuard).release();
    }

    @Test
    void guardFailureThrowsAlreadyRunningBusinessException() {
        when(policySyncExecutionGuard.tryAcquire()).thenReturn(false);

        BusinessException exception = catchThrowableOfType(
                () -> service().start("POLICY_SYNC"),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_JOB_ALREADY_RUNNING);
    }

    @Test
    void policySyncCompletesWithErrorsWhenIncrementalEmbeddingPartiallyFails() {
        allowJobStart();
        when(policySyncPipelineService.synchronize(eq(PolicySyncMode.INCREMENTAL), eq(PolicyCollectionExecutionType.MANUAL),
                anyJobProgressConsumer())).thenReturn(syncResult(2));

        AdminJobStatus status = service().start("POLICY_SYNC");

        assertThat(status.status()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(status.failedCount()).isEqualTo(2);
        verify(policySyncExecutionGuard).release();
    }

    @Test
    void releaseGuardAfterManualJobFailure() {
        allowJobStart();
        when(policySyncPipelineService.synchronize(eq(PolicySyncMode.INCREMENTAL), eq(PolicyCollectionExecutionType.MANUAL),
                anyJobProgressConsumer())).thenThrow(new BusinessException(ErrorCode.POLICY_SEARCH_NOT_READY));

        AdminJobStatus status = service().start("POLICY_SYNC");

        assertThat(status.status()).isEqualTo("FAILED");
        assertThat(status.message()).isEqualTo(ErrorCode.POLICY_SEARCH_NOT_READY.getMessage());
        verify(policySyncExecutionGuard).release();
    }

    private void allowJobStart() {
        when(policySyncExecutionGuard.tryAcquire()).thenReturn(true);
    }

    private AdminJobService service() {
        return new AdminJobService(collectionService, embeddingService, regionRebuildService, regionSynchronizationService,
                projectionService, lexicalIndexBuilder, regionCodeRepository,
                policySyncPipelineService, policySyncExecutionGuard, new SyncTaskExecutor());
    }

    private PolicySyncPipelineResult syncResult(int embeddingFailedCount) {
        return new PolicySyncPipelineResult(
                collectionResult("COMPLETED"),
                regionResult(2650, 0),
                new PolicySearchProjectionService.ProjectionRebuildResult(2650, 2650, 0),
                2650,
                new EmbeddingQueueResult(2650, 10, 2, 2638, 12),
                new EmbeddingProcessResult(12, 12 - embeddingFailedCount, embeddingFailedCount, 0, "COMPLETED"),
                new SearchReadinessResponse(true, 2650, 2650, 2650, 2648, List.of())
        );
    }

    private PolicyCollectionResult collectionResult(String status) {
        return new PolicyCollectionResult(1, 2650, 27, 27, 2650, 100, 2550, 0, status, status);
    }

    private PolicyRegionRebuildResult regionResult(long processed, long failed) {
        return new PolicyRegionRebuildResult(2650, processed, 10, 0, 0, 2640, failed, 0, 2650, 0, 0, 0);
    }

    private PolicyLexicalIndex index(int size) {
        PolicyLexicalIndex index = mock(PolicyLexicalIndex.class);
        when(index.size()).thenReturn(size);
        when(index.builtAt()).thenReturn(Instant.parse("2026-07-18T00:00:00Z"));
        return index;
    }

    private Consumer<com.weaone.themoa.common.dto.JobProgressUpdate> anyJobProgressConsumer() {
        return org.mockito.ArgumentMatchers.<Consumer<com.weaone.themoa.common.dto.JobProgressUpdate>>any();
    }

    private Consumer<PolicySearchProjectionService.ProjectionRebuildProgress> anyProjectionProgressConsumer() {
        return org.mockito.ArgumentMatchers.<Consumer<PolicySearchProjectionService.ProjectionRebuildProgress>>any();
    }
}
