package com.weaone.themoa.domain.policy.sync.service;

import com.weaone.themoa.common.dto.JobProgressUpdate;
import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
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
import com.weaone.themoa.domain.policy.rag.service.SearchReadinessService;
import com.weaone.themoa.domain.policy.region.config.RegionSyncProperties;
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PolicySyncPipelineServiceTest {
    private final YouthCenterPolicyCollectionService collectionService = mock(YouthCenterPolicyCollectionService.class);
    private final PolicyRegionRebuildService regionRebuildService = mock(PolicyRegionRebuildService.class);
    private final RegionSynchronizationService regionSynchronizationService = mock(RegionSynchronizationService.class);
    private final PolicySearchProjectionService projectionService = mock(PolicySearchProjectionService.class);
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder = mock(PolicyLexicalIndexBuilder.class);
    private final PolicyEmbeddingService embeddingService = mock(PolicyEmbeddingService.class);
    private final SearchReadinessService readinessService = mock(SearchReadinessService.class);
    private final RegionCodeRepository regionCodeRepository = mock(RegionCodeRepository.class);

    @Test
    void incrementalRunsRequiredStepsInOrderAndQueuesChangedEmbeddingsOnly() {
        stubSuccess(0);

        PolicySyncPipelineResult result = service().synchronize(
                PolicySyncMode.INCREMENTAL,
                PolicyCollectionExecutionType.MANUAL,
                null
        );

        assertThat(result.embeddingProcess().failedCount()).isZero();
        InOrder order = inOrder(regionCodeRepository, collectionService, lexicalIndexBuilder, regionRebuildService,
                projectionService, embeddingService, readinessService);
        order.verify(regionCodeRepository).count();
        order.verify(collectionService).collectAll(eq(PolicyCollectionExecutionType.MANUAL), anyJobProgressConsumer());
        order.verify(lexicalIndexBuilder).invalidate();
        order.verify(regionRebuildService).rebuildAll(anyJobProgressConsumer());
        order.verify(projectionService).rebuildAll(anyProjectionProgressConsumer());
        order.verify(lexicalIndexBuilder).refresh();
        order.verify(embeddingService).queueAll(eq(false), anyJobProgressConsumer());
        order.verify(embeddingService).processPending(anyEmbeddingProgressConsumer());
        order.verify(readinessService).readiness();
    }

    @Test
    void fullReindexQueuesAllEmbeddingsWithForce() {
        stubSuccess(0);

        service().synchronize(PolicySyncMode.FULL_REINDEX, PolicyCollectionExecutionType.MANUAL, null);

        verify(embeddingService).queueAll(eq(true), anyJobProgressConsumer());
    }

    @Test
    void scheduledExecutionPassesScheduledCollectionType() {
        stubSuccess(0);

        service().synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.SCHEDULED, null);

        verify(collectionService).collectAll(eq(PolicyCollectionExecutionType.SCHEDULED), anyJobProgressConsumer());
    }

    @Test
    void failedCollectionStopsLaterSteps() {
        when(regionCodeRepository.count()).thenReturn(1L);
        when(collectionService.collectAll(eq(PolicyCollectionExecutionType.MANUAL), anyJobProgressConsumer()))
                .thenReturn(collectionResult("FAILED"));

        BusinessException exception = catchThrowableOfType(
                () -> service().synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.MANUAL, null),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_EXTERNAL_API_ERROR);
        verifyNoInteractions(regionRebuildService, projectionService, embeddingService, readinessService);
    }

    @Test
    void failedRegionRebuildStopsProjectionAndLaterSteps() {
        when(regionCodeRepository.count()).thenReturn(1L);
        when(collectionService.collectAll(eq(PolicyCollectionExecutionType.MANUAL), anyJobProgressConsumer()))
                .thenReturn(collectionResult("COMPLETED"));
        when(regionRebuildService.rebuildAll(anyJobProgressConsumer())).thenReturn(regionResult(2650, 1));

        BusinessException exception = catchThrowableOfType(
                () -> service().synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.MANUAL, null),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_REGION_REBUILD_FAILED);
        verifyNoInteractions(projectionService, embeddingService, readinessService);
    }

    @Test
    void notReadySearchThrowsBusinessException() {
        stubSuccessUntilReadiness(false, 0);

        BusinessException exception = catchThrowableOfType(
                () -> service().synchronize(PolicySyncMode.INCREMENTAL, PolicyCollectionExecutionType.MANUAL, null),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_SEARCH_NOT_READY);
    }

    @Test
    void incrementalEmbeddingPartialFailureIsReturned() {
        stubSuccess(2);

        PolicySyncPipelineResult result = service().synchronize(
                PolicySyncMode.INCREMENTAL,
                PolicyCollectionExecutionType.MANUAL,
                null
        );

        assertThat(result.embeddingProcess().failedCount()).isEqualTo(2);
        verify(embeddingService).queueAll(eq(false), anyJobProgressConsumer());
        verify(embeddingService, never()).queueAll(eq(true), anyJobProgressConsumer());
    }

    private void stubSuccess(int embeddingFailedCount) {
        stubSuccessUntilReadiness(true, embeddingFailedCount);
    }

    private void stubSuccessUntilReadiness(boolean ready, int embeddingFailedCount) {
        when(regionCodeRepository.count()).thenReturn(1L);
        when(collectionService.collectAll(eq(PolicyCollectionExecutionType.MANUAL), anyJobProgressConsumer()))
                .thenReturn(collectionResult("COMPLETED"));
        when(collectionService.collectAll(eq(PolicyCollectionExecutionType.SCHEDULED), anyJobProgressConsumer()))
                .thenReturn(collectionResult("COMPLETED"));
        when(regionRebuildService.rebuildAll(anyJobProgressConsumer())).thenReturn(regionResult(2650, 0));
        when(projectionService.rebuildAll(anyProjectionProgressConsumer()))
                .thenReturn(new PolicySearchProjectionService.ProjectionRebuildResult(2650, 2650, 0));
        PolicyLexicalIndex index = index(2650);
        when(lexicalIndexBuilder.refresh()).thenReturn(index);
        when(embeddingService.queueAll(eq(false), anyJobProgressConsumer()))
                .thenReturn(new EmbeddingQueueResult(2650, 10, 2, 2638, 12));
        when(embeddingService.queueAll(eq(true), anyJobProgressConsumer()))
                .thenReturn(new EmbeddingQueueResult(2650, 2650, 0, 0, 2650));
        when(embeddingService.pendingCount()).thenReturn(12L);
        when(embeddingService.batchSize()).thenReturn(100);
        when(embeddingService.processPending(anyEmbeddingProgressConsumer()))
                .thenReturn(new EmbeddingProcessResult(12, 12 - embeddingFailedCount, embeddingFailedCount, 0, "COMPLETED"));
        when(readinessService.readiness()).thenReturn(new SearchReadinessResponse(ready, 2650, 2650, 2650, 2648, List.of()));
    }

    private PolicySyncPipelineService service() {
        return new PolicySyncPipelineService(collectionService, regionRebuildService, regionSynchronizationService,
                projectionService, lexicalIndexBuilder, embeddingService, readinessService, regionCodeRepository,
                regionSyncProperties());
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

    private RegionSyncProperties regionSyncProperties() {
        return new RegionSyncProperties(false, false, "0 0 4 1 * *",
                Duration.ofMillis(100), Duration.ofSeconds(5), Duration.ofSeconds(20), 3,
                new RegionSyncProperties.Sgis("https://sgisapi.mods.go.kr", "", ""));
    }

    private Consumer<JobProgressUpdate> anyJobProgressConsumer() {
        return org.mockito.ArgumentMatchers.<Consumer<JobProgressUpdate>>any();
    }

    private Consumer<PolicySearchProjectionService.ProjectionRebuildProgress> anyProjectionProgressConsumer() {
        return org.mockito.ArgumentMatchers.<Consumer<PolicySearchProjectionService.ProjectionRebuildProgress>>any();
    }

    private Consumer<PolicyEmbeddingService.EmbeddingProgress> anyEmbeddingProgressConsumer() {
        return org.mockito.ArgumentMatchers.<Consumer<PolicyEmbeddingService.EmbeddingProgress>>any();
    }
}
