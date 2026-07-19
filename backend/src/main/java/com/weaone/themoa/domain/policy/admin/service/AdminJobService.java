package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.policy.service.PolicyCollectionResult;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.service.PolicyRegionRebuildResult;
import com.weaone.themoa.domain.policy.policy.service.PolicyRegionRebuildService;
import com.weaone.themoa.domain.policy.policy.service.YouthCenterPolicyCollectionService;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingProcessResult;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingQueueResult;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmbeddingService;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndexBuilder;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService;
import com.weaone.themoa.domain.policy.region.config.RegionSyncProperties;
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationResult;
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminJobService {
    private final YouthCenterPolicyCollectionService collectionService;
    private final PolicyEmbeddingService embeddingService;
    private final PolicyRegionRebuildService regionRebuildService;
    private final RegionSynchronizationService regionSynchronizationService;
    private final PolicySearchProjectionService projectionService;
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder;
    private final SearchReadinessService readinessService;
    private final RegionCodeRepository regionCodeRepository;
    private final RegionSyncProperties regionSyncProperties;
    private final TaskExecutor adminJobExecutor;
    private final Map<String, MutableJob> jobs = new ConcurrentHashMap<>();

    public AdminJobService(YouthCenterPolicyCollectionService collectionService, PolicyEmbeddingService embeddingService,
                           PolicyRegionRebuildService regionRebuildService,
                           RegionSynchronizationService regionSynchronizationService,
                           PolicySearchProjectionService projectionService,
                           PolicyLexicalIndexBuilder lexicalIndexBuilder,
                           SearchReadinessService readinessService,
                           RegionCodeRepository regionCodeRepository,
                           RegionSyncProperties regionSyncProperties,
                           @Qualifier("adminJobExecutor") TaskExecutor adminJobExecutor) {
        this.collectionService = collectionService;
        this.embeddingService = embeddingService;
        this.regionRebuildService = regionRebuildService;
        this.regionSynchronizationService = regionSynchronizationService;
        this.projectionService = projectionService;
        this.lexicalIndexBuilder = lexicalIndexBuilder;
        this.readinessService = readinessService;
        this.regionCodeRepository = regionCodeRepository;
        this.regionSyncProperties = regionSyncProperties;
        this.adminJobExecutor = adminJobExecutor;
    }

    public AdminJobStatus start(String type) {
        if (jobs.values().stream().anyMatch(job -> job.type.equals(type) && "RUNNING".equals(job.status))) {
            throw new IllegalStateException("JOB_ALREADY_RUNNING");
        }
        MutableJob job = new MutableJob(UUID.randomUUID().toString(), type);
        jobs.put(job.id, job);
        CompletableFuture.runAsync(() -> run(job), runnable -> adminJobExecutor.execute(runnable));
        return job.snapshot();
    }

    private void run(MutableJob job) {
        try {
            switch (job.type) {
                case "POLICY_COLLECTION" -> {
                    job.update(new JobProgressUpdate("CONNECTING", "온통청년 연결 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "온통청년 API 연결 중"));
                    PolicyCollectionResult result = collectionService.collectAll(job::update);
                    job.processed = result.receivedCount();
                    job.success = result.insertedCount() + result.updatedCount();
                    job.failed = result.failedCount();
                    job.message = result.status();
                    lexicalIndexBuilder.invalidate();
                    if ("FAILED".equals(result.status())) {
                        job.status = "FAILED";
                    }
                }
                case "EMBEDDING_QUEUE" -> {
                    job.update(new JobProgressUpdate("QUEUEING", "임베딩 대기열 등록 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "활성 정책을 조회하고 있습니다."));
                    EmbeddingQueueResult result = embeddingService.queueAll(false, job::update);
                    job.total = result.activePolicyCount();
                    job.success = result.newlyQueuedCount() + result.requeuedCount();
                    job.remaining = result.pendingCountAfter();
                    job.message = "QUEUE_COMPLETED";
                }
                case "EMBEDDING_PROCESS" -> {
                    long initialPending = embeddingService.pendingCount();
                    int batchSize = Math.max(1, embeddingService.batchSize());
                    int totalBatches = (int) Math.ceil((double) initialPending / batchSize);
                    job.total = initialPending;
                    job.remaining = initialPending;
                    job.totalBatches = totalBatches;
                    job.update(new JobProgressUpdate("PREPARING", "임베딩 준비 중", initialPending, 0, 0, 0, 0, 0, 0, totalBatches, null, 0, 0, "PENDING 임베딩을 준비하고 있습니다."));
                    EmbeddingProcessResult result = embeddingService.processPending(progress -> {
                        job.update(new JobProgressUpdate("PROCESSING", "OpenAI 임베딩 생성 중", initialPending,
                                progress.processedCount(), progress.successCount(), progress.failedCount(), 0, 0,
                                Math.min(totalBatches, (int) Math.ceil((double) progress.processedCount() / batchSize)),
                                totalBatches, null, 0, 0, "임베딩을 처리하고 있습니다."));
                        job.remaining = progress.pendingCountAfter();
                    });
                    job.processed = result.processedCount();
                    job.success = result.successCount();
                    job.failed = result.failedCount();
                    job.remaining = result.pendingCountAfter();
                    job.message = result.message();
                    if (initialPending > 0 && result.processedCount() == 0 && !"COMPLETED".equals(result.message())) {
                        throw new IllegalStateException(result.message());
                    }
                }
                case "EMBEDDING_RETRY_FAILED" -> {
                    int count = embeddingService.retryFailed();
                    job.success = count;
                    job.message = "FAILED_REQUEUED";
                }
                case "POLICY_REGION_REBUILD" -> {
                    if (regionCodeRepository.count() <= 0) {
                        throw new IllegalStateException("REGION_CATALOG_EMPTY: 전국 행정지역 동기화를 먼저 실행하세요.");
                    }
                    job.update(new JobProgressUpdate("REBUILDING", "지역 재계산 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "정책 지역을 다시 계산합니다."));
                    PolicyRegionRebuildResult result = regionRebuildService.rebuildAll(job::update);
                    job.total = result.totalCount();
                    job.processed = result.processedCount();
                    job.success = result.changedCount();
                    job.failed = result.failedCount();
                    job.remaining = Math.max(0, result.totalCount() - result.processedCount());
                    job.message = "REGION_REBUILD_COMPLETED changed=" + result.changedCount()
                            + ", nationwideToRegion=" + result.nationwideToRegionCount()
                            + ", nationwideToUnknown=" + result.nationwideToUnknownCount()
                            + ", unchanged=" + result.unchangedCount()
                            + ", pendingQueued=" + result.pendingQueuedCount();
                }
                case "REGION_CATALOG_SYNC" -> {
                    job.update(new JobProgressUpdate("AUTHENTICATING", "SGIS 인증 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "SGIS 인증 중입니다."));
                    RegionSynchronizationResult result = regionSynchronizationService.synchronize(job::update);
                    job.total = result.provinceReceivedCount() + result.childReceivedCount();
                    job.processed = job.total;
                    job.success = result.insertedCount() + result.updatedCount() + result.unchangedCount();
                    job.failed = result.failedCount();
                    job.remaining = 0;
                    job.message = "REGION_CATALOG_SYNC_COMPLETED provinces=" + result.provinceReceivedCount()
                            + ", children=" + result.childReceivedCount()
                            + ", inserted=" + result.insertedCount()
                            + ", updated=" + result.updatedCount()
                            + ", unchanged=" + result.unchangedCount()
                            + ", failedProvinceCodes=" + result.failedProvinceCodes();
                }
                case "REGION_CATALOG_REPAIR" -> {
                    job.update(new JobProgressUpdate("REPAIRING", "지역 카탈로그 복구 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "SGIS 표준 지역을 다시 upsert합니다."));
                    RegionSynchronizationResult result = regionSynchronizationService.synchronize(job::update);
                    job.total = result.provinceReceivedCount();
                    job.processed = result.provinceReceivedCount();
                    job.success = result.insertedCount() + result.updatedCount() + result.unchangedCount();
                    job.failed = result.failedCount();
                    job.remaining = 0;
                    job.message = "REGION_CATALOG_REPAIR_COMPLETED inserted=" + result.insertedCount()
                            + ", updated=" + result.updatedCount()
                            + ", unchanged=" + result.unchangedCount()
                            + ", failedProvinceCodes=" + result.failedProvinceCodes()
                            + ". 기존 FK 연결 지역은 삭제하지 않았습니다.";
                }
                case "SEARCH_PROJECTION_REBUILD" -> runSearchProjectionRebuild(job);
                case "SEARCH_INDEX_REFRESH" -> runSearchIndexRefresh(job);
                case "FULL_REINDEX" -> {
                    runFullReindex(job);
                }
                default -> throw new IllegalArgumentException("UNSUPPORTED_JOB_TYPE: " + job.type);
            }
            if ("RUNNING".equals(job.status)) {
                job.status = job.failed > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED";
            }
            job.completedAt = Instant.now();
        } catch (RuntimeException ex) {
            job.status = "FAILED";
            job.completedAt = Instant.now();
            job.message = ex.getMessage();
        }
    }

    private void runSearchProjectionRebuild(MutableJob job) {
        job.update(new JobProgressUpdate("SEARCH_PROJECTION_REBUILDING", "Search Projection 생성 중",
                0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "활성 정책 Search Projection을 다시 생성합니다."));
        var result = projectionService.rebuildAll(progress -> job.update(new JobProgressUpdate(
                "SEARCH_PROJECTION_REBUILDING", "Search Projection 생성 중",
                progress.total(), progress.processed(), progress.processed(), 0, 0, 0,
                0, 0, null, 0, 0, "Search Projection 생성 중")));
        job.update(new JobProgressUpdate("SEARCH_INDEX_REFRESHING", "검색 인덱스 생성 중",
                result.total(), result.processed(), result.processed(), 0, 0, 0, 0, 0, null, 0, 0,
                "Projection 생성 후 검색 인덱스를 갱신합니다."));
        var index = lexicalIndexBuilder.refresh();
        job.total = result.total();
        job.processed = result.processed();
        job.success = result.processed();
        job.failed = 0;
        job.remaining = 0;
        job.message = "SEARCH_PROJECTION_REBUILD_COMPLETED projectionCount=" + result.processed()
                + ", missingSnapshot=" + result.missingSnapshot()
                + ", indexDocumentCount=" + index.size();
    }

    private void runSearchIndexRefresh(MutableJob job) {
        job.update(new JobProgressUpdate("SEARCH_INDEX_REFRESHING", "검색 인덱스 생성 중",
                0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0,
                "활성 Search Projection으로 검색 인덱스를 다시 생성합니다."));
        var index = lexicalIndexBuilder.refresh();
        job.total = index.size();
        job.processed = index.size();
        job.success = index.size();
        job.failed = 0;
        job.remaining = 0;
        job.message = "SEARCH_INDEX_REFRESH_COMPLETED documentCount=" + index.size()
                + ", builtAt=" + index.builtAt();
    }

    private void runFullReindex(MutableJob job) {
        updateFullReindexStage(job, 1, "CHECKING_REGION_CATALOG", "지역 카탈로그 준비 상태 확인 중",
                "지역 카탈로그 준비 상태를 확인합니다.");
        ensureRegionCatalogReady(job);

        updateFullReindexStage(job, 2, "POLICY_COLLECTION", "정책 수집 중", "정책 API 전체 수집을 실행합니다.");
        PolicyCollectionResult collection = collectionService.collectAll(job::update);
        lexicalIndexBuilder.invalidate();
        if ("FAILED".equals(collection.status())) {
            throw new IllegalStateException("POLICY_COLLECTION_FAILED");
        }

        updateFullReindexStage(job, 3, "POLICY_REGION_REBUILD", "정책 지역 계산 중", "저장된 지역 코드를 기준으로 정책 대상 지역을 재분류합니다.");
        PolicyRegionRebuildResult region = regionRebuildService.rebuildAll(job::update);
        if (region.failedCount() > 0) {
            throw new IllegalStateException("POLICY_REGION_REBUILD_FAILED failed=" + region.failedCount());
        }

        updateFullReindexStage(job, 4, "SEARCH_PROJECTION_REBUILD", "Search Projection 생성 중", "Search Projection 전체 재생성을 실행합니다.");
        var projection = projectionService.rebuildAll(progress -> job.update(new JobProgressUpdate(
                "SEARCH_PROJECTION_REBUILD", "Search Projection 생성 중",
                progress.total(), progress.processed(), progress.processed(), 0, 0, 0,
                0, 0, null, 0, 0, "Search Projection 생성 중")));

        updateFullReindexStage(job, 5, "SEARCH_INDEX_REFRESH", "검색 인덱스 생성 중", "Lexical Index를 갱신합니다.");
        var index = lexicalIndexBuilder.refresh();

        updateFullReindexStage(job, 6, "EMBEDDING_QUEUE", "Embedding 대기열 등록 중", "전체 정책 Embedding 대기열을 등록합니다.");
        EmbeddingQueueResult queue = embeddingService.queueAll(true, job::update);

        updateFullReindexStage(job, 7, "EMBEDDING_PROCESS", "Embedding 처리 중", "PENDING Embedding을 처리합니다.");
        long initialPending = embeddingService.pendingCount();
        int batchSize = Math.max(1, embeddingService.batchSize());
        int totalBatches = (int) Math.ceil((double) initialPending / batchSize);
        EmbeddingProcessResult process = embeddingService.processPending(progress -> job.update(new JobProgressUpdate(
                "EMBEDDING_PROCESS", "Embedding 처리 중", initialPending,
                progress.processedCount(), progress.successCount(), progress.failedCount(), 0, 0,
                Math.min(totalBatches, (int) Math.ceil((double) progress.processedCount() / batchSize)),
                totalBatches, null, 0, 0, "Embedding 처리 중")));
        if (process.failedCount() > 0) {
            throw new IllegalStateException("EMBEDDING_PROCESS_FAILED failed=" + process.failedCount());
        }
        if (initialPending > 0 && process.processedCount() == 0 && !"COMPLETED".equals(process.message())) {
            throw new IllegalStateException(process.message());
        }

        updateFullReindexStage(job, 8, "SEARCH_READINESS_CHECK", "검색 준비 상태 확인 중", "최종 검색 준비 상태를 확인합니다.");
        var readiness = readinessService.readiness();
        if (!readiness.ready()) {
            throw new IllegalStateException("SEARCH_DATA_NOT_READY missingSteps=" + readiness.missingSteps());
        }

        job.total = queue.activePolicyCount();
        job.processed = collection.receivedCount() + region.processedCount() + projection.processed() + process.processedCount();
        job.success = collection.insertedCount() + collection.updatedCount() + region.changedCount()
                + projection.processed() + index.size() + process.successCount();
        job.failed = collection.failedCount() + region.failedCount() + process.failedCount();
        job.remaining = process.pendingCountAfter();
        job.message = "FULL_REINDEX_COMPLETED projectionCount=" + projection.processed()
                + ", indexDocumentCount=" + index.size()
                + ", syncedEmbeddingCount=" + readiness.syncedEmbeddingCount();
    }

    private void ensureRegionCatalogReady(MutableJob job) {
        if (regionCodeRepository.count() > 0) {
            return;
        }
        if (!regionSyncProperties.enabled() || !regionSyncProperties.credentialsConfigured()) {
            throw new IllegalStateException("REGION_CATALOG_EMPTY: SGIS_REGION_SYNC_ENABLED와 SGIS 인증 정보를 확인하세요.");
        }
        job.update(new JobProgressUpdate("REGION_CATALOG_SYNC", "전국 행정지역 동기화 중",
                8, 1, 1, 0, 0, 0, 0, 0, null, 0, 0,
                "지역 카탈로그가 비어 있어 먼저 전국 행정지역을 동기화합니다."));
        RegionSynchronizationResult result = regionSynchronizationService.synchronize(job::update);
        if (result.failedCount() > 0 || regionCodeRepository.count() <= 0) {
            throw new IllegalStateException("REGION_CATALOG_SYNC_FAILED failed=" + result.failedCount()
                    + ", failedProvinceCodes=" + result.failedProvinceCodes());
        }
    }

    private void updateFullReindexStage(MutableJob job, int step, String stage, String stageLabel, String message) {
        job.update(new JobProgressUpdate(stage, stageLabel, 8, step - 1, step - 1, 0, 8 - step + 1,
                0, 0, 0, null, 0, 0, message));
    }

    public Optional<AdminJobStatus> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId)).map(MutableJob::snapshot);
    }

    public Optional<AdminJobStatus> latest() {
        return jobs.values().stream()
                .max(Comparator.comparing(job -> job.startedAt))
                .map(MutableJob::snapshot);
    }

    private static class MutableJob {
        private final String id;
        private final String type;
        private final Instant startedAt = Instant.now();
        private volatile Instant updatedAt = startedAt;
        private volatile Instant completedAt;
        private String status = "RUNNING";
        private String stage;
        private String stageLabel;
        private long total;
        private long processed;
        private long success;
        private long failed;
        private long remaining;
        private int currentPage;
        private int totalPages;
        private int currentBatch;
        private int totalBatches;
        private String currentItem;
        private long apiRequestCount;
        private long retryCount;
        private String message = "";

        private MutableJob(String id, String type) {
            this.id = id;
            this.type = type;
        }

        private synchronized void update(JobProgressUpdate update) {
            this.stage = update.stage();
            this.stageLabel = update.stageLabel();
            this.total = update.total() > 0 ? update.total() : this.total;
            this.processed = update.processed();
            this.success = update.success();
            this.failed = update.failed();
            this.remaining = Math.max(0, this.total - this.processed);
            this.currentPage = update.currentPage();
            this.totalPages = update.totalPages();
            this.currentBatch = update.currentBatch();
            this.totalBatches = update.totalBatches();
            this.currentItem = update.currentItem();
            this.apiRequestCount = update.apiRequestCount();
            this.retryCount = update.retryCount();
            this.message = update.message();
            this.updatedAt = Instant.now();
        }

        private AdminJobStatus snapshot() {
            long elapsed = Duration.between(startedAt, completedAt == null ? Instant.now() : completedAt).toMillis();
            Double throughput = elapsed > 0 && processed > 0 ? processed / (elapsed / 1000.0) : null;
            Long eta = throughput != null && throughput > 0 && remaining > 0 ? Math.round(remaining / throughput) : null;
            Integer percent = percent();
            return new AdminJobStatus(id, type, status, stage, stageLabel, percent, percent, total <= 0,
                    total, processed, success, failed, remaining, currentPage, totalPages, currentBatch, totalBatches,
                    currentItem, apiRequestCount, retryCount, startedAt, updatedAt, completedAt, elapsed, eta, throughput, message);
        }

        private Integer percent() {
            if (total <= 0) return null;
            if ("COMPLETED".equals(status) || "COMPLETED_WITH_ERRORS".equals(status)) return 100;
            int value = (int) Math.floor((double) Math.min(processed, total) * 100 / total);
            return "RUNNING".equals(status) ? Math.min(value, 99) : Math.max(0, Math.min(value, 100));
        }
    }
}
