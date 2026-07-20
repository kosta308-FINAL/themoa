package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.rag.service.SearchReadinessService;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.service.PolicyCollectionResult;
import com.weaone.themoa.domain.policy.policy.service.PolicyRegionRebuildResult;
import com.weaone.themoa.domain.policy.policy.service.PolicyRegionRebuildService;
import com.weaone.themoa.domain.policy.policy.service.YouthCenterPolicyCollectionService;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingProcessResult;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingQueueResult;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmbeddingService;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndex;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndexBuilder;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService;
import com.weaone.themoa.domain.policy.region.config.RegionSyncProperties;
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminJobServiceTest {
    private final YouthCenterPolicyCollectionService collectionService = mock(YouthCenterPolicyCollectionService.class);
    private final PolicyEmbeddingService embeddingService = mock(PolicyEmbeddingService.class);
    private final PolicyRegionRebuildService regionRebuildService = mock(PolicyRegionRebuildService.class);
    private final RegionSynchronizationService regionSynchronizationService = mock(RegionSynchronizationService.class);
    private final PolicySearchProjectionService projectionService = mock(PolicySearchProjectionService.class);
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder = mock(PolicyLexicalIndexBuilder.class);
    private final SearchReadinessService readinessService = mock(SearchReadinessService.class);
    private final RegionCodeRepository regionCodeRepository = mock(RegionCodeRepository.class);

    @Test
    void searchProjectionRebuildRefreshesLexicalIndexAndCompletes() {
        when(projectionService.rebuildAll(anyProjectionProgressConsumer()))
                .thenReturn(new PolicySearchProjectionService.ProjectionRebuildResult(2650, 2650, 0));
        PolicyLexicalIndex index = index(2650);
        when(lexicalIndexBuilder.refresh()).thenReturn(index);

        AdminJobStatus status = service().start("SEARCH_PROJECTION_REBUILD");

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.message()).contains("projectionCount=2650", "indexDocumentCount=2650");
        verify(projectionService).rebuildAll(anyProjectionProgressConsumer());
        verify(lexicalIndexBuilder).refresh();
    }

    @Test
    void searchIndexRefreshBuildsIndexAndCompletes() {
        PolicyLexicalIndex index = index(2650);
        when(lexicalIndexBuilder.refresh()).thenReturn(index);

        AdminJobStatus status = service().start("SEARCH_INDEX_REFRESH");

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.message()).contains("documentCount=2650");
        verify(lexicalIndexBuilder).refresh();
    }

    @Test
    void fullReindexRunsRequiredStepsInOrder() {
        when(regionCodeRepository.count()).thenReturn(1L);
        when(collectionService.collectAll(any())).thenReturn(collectionResult("COMPLETED"));
        when(regionRebuildService.rebuildAll(any())).thenReturn(regionResult(2650, 0));
        when(projectionService.rebuildAll(anyProjectionProgressConsumer()))
                .thenReturn(new PolicySearchProjectionService.ProjectionRebuildResult(2650, 2650, 0));
        PolicyLexicalIndex index = index(2650);
        when(lexicalIndexBuilder.refresh()).thenReturn(index);
        when(embeddingService.queueAll(eq(true), any())).thenReturn(new EmbeddingQueueResult(2650, 2650, 0, 0, 2650));
        when(embeddingService.pendingCount()).thenReturn(2650L);
        when(embeddingService.batchSize()).thenReturn(100);
        when(embeddingService.processPending(anyEmbeddingProgressConsumer()))
                .thenReturn(new EmbeddingProcessResult(2650, 2650, 0, 0, "COMPLETED"));
        when(readinessService.readiness()).thenReturn(new SearchReadinessResponse(true, 2650, 2650, 2650, 2650, List.of()));

        AdminJobStatus status = service().start("FULL_REINDEX");

        assertThat(status.status()).isEqualTo("COMPLETED");
        InOrder order = inOrder(collectionService, regionRebuildService, projectionService, lexicalIndexBuilder,
                embeddingService, readinessService);
        order.verify(collectionService).collectAll(any());
        order.verify(regionRebuildService).rebuildAll(any());
        order.verify(projectionService).rebuildAll(anyProjectionProgressConsumer());
        order.verify(lexicalIndexBuilder).refresh();
        order.verify(embeddingService).queueAll(eq(true), any());
        order.verify(embeddingService).processPending(anyEmbeddingProgressConsumer());
        order.verify(readinessService).readiness();
    }

    @Test
    void fullReindexFailsWhenMiddleStepFails() {
        when(regionCodeRepository.count()).thenReturn(1L);
        when(collectionService.collectAll(any())).thenReturn(collectionResult("COMPLETED"));
        when(regionRebuildService.rebuildAll(any())).thenReturn(regionResult(2650, 1));

        AdminJobStatus status = service().start("FULL_REINDEX");

        assertThat(status.status()).isEqualTo("FAILED");
        assertThat(status.message()).contains("정책 지역 재분류를 완료하지 못했습니다.");
    }

    private AdminJobService service() {
        return new AdminJobService(collectionService, embeddingService, regionRebuildService, regionSynchronizationService,
                projectionService, lexicalIndexBuilder, readinessService, regionCodeRepository, regionSyncProperties(),
                new SyncTaskExecutor());
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

    @SuppressWarnings("unchecked")
    private Consumer<PolicySearchProjectionService.ProjectionRebuildProgress> anyProjectionProgressConsumer() {
        return any(Consumer.class);
    }

    @SuppressWarnings("unchecked")
    private Consumer<PolicyEmbeddingService.EmbeddingProgress> anyEmbeddingProgressConsumer() {
        return any(Consumer.class);
    }
}
