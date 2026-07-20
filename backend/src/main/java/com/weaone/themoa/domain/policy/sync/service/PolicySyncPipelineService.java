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
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationResult;
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Consumer;

@Service
public class PolicySyncPipelineService {
    private static final int TOTAL_STEPS = 8;

    private final YouthCenterPolicyCollectionService collectionService;
    private final PolicyRegionRebuildService regionRebuildService;
    private final RegionSynchronizationService regionSynchronizationService;
    private final PolicySearchProjectionService projectionService;
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder;
    private final PolicyEmbeddingService embeddingService;
    private final SearchReadinessService readinessService;
    private final RegionCodeRepository regionCodeRepository;
    private final RegionSyncProperties regionSyncProperties;

    public PolicySyncPipelineService(YouthCenterPolicyCollectionService collectionService,
                                     PolicyRegionRebuildService regionRebuildService,
                                     RegionSynchronizationService regionSynchronizationService,
                                     PolicySearchProjectionService projectionService,
                                     PolicyLexicalIndexBuilder lexicalIndexBuilder,
                                     PolicyEmbeddingService embeddingService,
                                     SearchReadinessService readinessService,
                                     RegionCodeRepository regionCodeRepository,
                                     RegionSyncProperties regionSyncProperties) {
        this.collectionService = collectionService;
        this.regionRebuildService = regionRebuildService;
        this.regionSynchronizationService = regionSynchronizationService;
        this.projectionService = projectionService;
        this.lexicalIndexBuilder = lexicalIndexBuilder;
        this.embeddingService = embeddingService;
        this.readinessService = readinessService;
        this.regionCodeRepository = regionCodeRepository;
        this.regionSyncProperties = regionSyncProperties;
    }

    public PolicySyncPipelineResult synchronize(PolicySyncMode mode,
                                                PolicyCollectionExecutionType executionType,
                                                Consumer<JobProgressUpdate> progressConsumer) {
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(executionType, "executionType must not be null");

        updateStage(progressConsumer, 1, "CHECKING_REGION_CATALOG", "지역 카탈로그 준비 상태 확인 중",
                "지역 카탈로그 준비 상태를 확인합니다.");
        ensureRegionCatalogReady(progressConsumer);

        updateStage(progressConsumer, 2, "POLICY_COLLECTION", "정책 수집 중",
                "정책 API 전체 수집을 실행합니다.");
        PolicyCollectionResult collection = collectionService.collectAll(executionType, progressConsumer);
        lexicalIndexBuilder.invalidate();
        if ("FAILED".equals(collection.status())) {
            throw new BusinessException(ErrorCode.POLICY_EXTERNAL_API_ERROR);
        }

        updateStage(progressConsumer, 3, "POLICY_REGION_REBUILD", "정책 지역 계산 중",
                "저장된 지역 코드를 기준으로 정책 대상 지역을 재분류합니다.");
        PolicyRegionRebuildResult region = regionRebuildService.rebuildAll(progressConsumer);
        if (region.failedCount() > 0) {
            throw new BusinessException(ErrorCode.POLICY_REGION_REBUILD_FAILED);
        }

        updateStage(progressConsumer, 4, "SEARCH_PROJECTION_REBUILD", "Search Projection 생성 중",
                "Search Projection 전체 재생성을 실행합니다.");
        PolicySearchProjectionService.ProjectionRebuildResult projection = projectionService.rebuildAll(progress -> notify(progressConsumer,
                new JobProgressUpdate("SEARCH_PROJECTION_REBUILD", "Search Projection 생성 중",
                        progress.total(), progress.processed(), progress.processed(), 0, 0, 0,
                        0, 0, null, 0, 0, "Search Projection 생성 중")));

        updateStage(progressConsumer, 5, "SEARCH_INDEX_REFRESH", "검색 인덱스 생성 중",
                "Lexical Index를 갱신합니다.");
        PolicyLexicalIndex index = lexicalIndexBuilder.refresh();

        updateStage(progressConsumer, 6, "EMBEDDING_QUEUE", "Embedding 대기열 등록 중",
                mode.forceEmbedding() ? "전체 정책 Embedding 대기열을 등록합니다." : "변경된 정책 Embedding 대기열을 등록합니다.");
        EmbeddingQueueResult queue = embeddingService.queueAll(mode.forceEmbedding(), progressConsumer);

        updateStage(progressConsumer, 7, "EMBEDDING_PROCESS", "Embedding 처리 중",
                "PENDING Embedding을 처리합니다.");
        long initialPending = embeddingService.pendingCount();
        int batchSize = Math.max(1, embeddingService.batchSize());
        int totalBatches = (int) Math.ceil((double) initialPending / batchSize);
        EmbeddingProcessResult process = embeddingService.processPending(progress -> notify(progressConsumer,
                new JobProgressUpdate("EMBEDDING_PROCESS", "Embedding 처리 중", initialPending,
                        progress.processedCount(), progress.successCount(), progress.failedCount(), 0, 0,
                        Math.min(totalBatches, (int) Math.ceil((double) progress.processedCount() / batchSize)),
                        totalBatches, null, 0, 0, "Embedding 처리 중")));
        if (initialPending > 0 && process.processedCount() == 0 && !"COMPLETED".equals(process.message())) {
            throw new BusinessException(ErrorCode.POLICY_VECTOR_STORE_NOT_READY);
        }
        if (mode == PolicySyncMode.FULL_REINDEX && process.failedCount() > 0) {
            throw new BusinessException(ErrorCode.POLICY_VECTOR_STORE_NOT_READY);
        }

        updateStage(progressConsumer, 8, "SEARCH_READINESS_CHECK", "검색 준비 상태 확인 중",
                "최종 검색 준비 상태를 확인합니다.");
        SearchReadinessResponse readiness = readinessService.readiness();
        if (!readiness.ready()) {
            throw new BusinessException(ErrorCode.POLICY_SEARCH_NOT_READY);
        }

        return new PolicySyncPipelineResult(collection, region, projection, index.size(), queue, process, readiness);
    }

    private void ensureRegionCatalogReady(Consumer<JobProgressUpdate> progressConsumer) {
        if (regionCodeRepository.count() > 0) {
            return;
        }
        if (!regionSyncProperties.enabled() || !regionSyncProperties.credentialsConfigured()) {
            throw new BusinessException(ErrorCode.POLICY_REGION_SYNC_CONFIG_REQUIRED);
        }
        notify(progressConsumer, new JobProgressUpdate("REGION_CATALOG_SYNC", "전국 행정지역 동기화 중",
                TOTAL_STEPS, 1, 1, 0, 0, 0, 0, 0, null, 0, 0,
                "지역 카탈로그가 비어 있어 먼저 전국 행정지역을 동기화합니다."));
        RegionSynchronizationResult result = regionSynchronizationService.synchronize(progressConsumer);
        if (result.failedCount() > 0 || regionCodeRepository.count() <= 0) {
            throw new BusinessException(ErrorCode.POLICY_REGION_SYNC_FAILED);
        }
    }

    private void updateStage(Consumer<JobProgressUpdate> progressConsumer, int step, String stage, String stageLabel, String message) {
        notify(progressConsumer, new JobProgressUpdate(stage, stageLabel, TOTAL_STEPS, step - 1, step - 1, 0,
                TOTAL_STEPS - step + 1, 0, 0, 0, null, 0, 0, message));
    }

    private void notify(Consumer<JobProgressUpdate> progressConsumer, JobProgressUpdate update) {
        if (progressConsumer != null) {
            progressConsumer.accept(update);
        }
    }
}
