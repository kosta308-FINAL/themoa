package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.PolicyGenderClassification;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicyGenderClassificationRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.config.PolicyGenderClassificationProperties;
import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantScope;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderClassificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
public class PolicyGenderAudienceClassifier {
    private static final Logger log = LoggerFactory.getLogger(PolicyGenderAudienceClassifier.class);

    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicyGenderClassificationRepository classificationRepository;
    private final PolicyGenderAiClient aiClient;
    private final PolicyGenderClassificationPolicy classificationPolicy;
    private final PolicyGenderSourceHasher sourceHasher;
    private final PolicyGenderClassificationProperties properties;

    @Autowired
    public PolicyGenderAudienceClassifier(PolicySearchProjectionRepository projectionRepository,
                                          PolicyGenderClassificationRepository classificationRepository,
                                          PolicyGenderAiClient aiClient,
                                          PolicyGenderClassificationPolicy classificationPolicy,
                                          PolicyGenderSourceHasher sourceHasher,
                                          PolicyGenderClassificationProperties properties) {
        this.projectionRepository = projectionRepository;
        this.classificationRepository = classificationRepository;
        this.aiClient = aiClient;
        this.classificationPolicy = classificationPolicy;
        this.sourceHasher = sourceHasher;
        this.properties = properties;
    }

    public PolicyGenderAudienceClassifier(PolicySearchProjectionRepository projectionRepository,
                                          PolicyGenderClassificationRepository classificationRepository,
                                          PolicyGenderAiClient aiClient,
                                          PolicyGenderClassificationPolicy classificationPolicy,
                                          PolicyGenderSourceHasher sourceHasher) {
        this(projectionRepository, classificationRepository, aiClient, classificationPolicy, sourceHasher,
                new PolicyGenderClassificationProperties());
    }

    @Transactional(readOnly = true)
    public Map<Integer, PolicyGenderClassificationResult> findClassifications(Collection<Integer> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, PolicyGenderClassificationResult> results = new LinkedHashMap<>();
        classificationRepository.findAllById(policyIds).forEach(classification ->
                results.put(classification.getPolicyId(), classificationPolicy.normalize(classification.toResult())));
        policyIds.forEach(policyId -> results.putIfAbsent(policyId,
                PolicyGenderClassificationResult.unknown("저장된 성별 분류가 없습니다.")));
        return results;
    }

    @Transactional
    public GenderClassificationRebuildResult classifyMissingOrStale() {
        return classifyMissingOrStale(null);
    }

    @Transactional
    public GenderClassificationRebuildResult classifyMissingOrStale(Consumer<GenderClassificationProgress> progressConsumer) {
        int total = 0;
        int processed = 0;
        int success = 0;
        int failed = 0;
        int skipped = 0;
        int unknown = 0;
        int apiRequests = 0;
        int retries = 0;
        Map<Integer, PolicyGenderClassification> existing = new LinkedHashMap<>();
        classificationRepository.findAll().forEach(classification -> existing.put(classification.getPolicyId(), classification));
        List<ClassificationCandidate> candidates = new ArrayList<>();
        for (PolicySearchProjection projection : projectionRepository.findAllActive()) {
            total++;
            String sourceHash = sourceHasher.hash(projection);
            PolicyGenderClassification current = existing.get(projection.getPolicyId());
            if (current != null && !current.stale(PolicyGenderClassificationPolicy.VERSION, sourceHash)) {
                skipped++;
                continue;
            }
            candidates.add(new ClassificationCandidate(projection, current, sourceHash));
        }
        int totalBatches = candidates.isEmpty() ? 0
                : (int) Math.ceil((double) candidates.size() / properties.getBatchSize());
        publish(progressConsumer, total, processed, success, failed, skipped, unknown, 0, totalBatches,
                apiRequests, retries, "성별 분류 대상 계산 완료");
        if (!properties.isEnabled()) {
            skipped += candidates.size();
            publish(progressConsumer, total, processed, success, failed, skipped, unknown, totalBatches, totalBatches,
                    apiRequests, retries, "성별 분류 기능이 비활성화되어 미분류 정책을 건너뜁니다.");
            return new GenderClassificationRebuildResult(total, processed, success, failed, skipped, unknown,
                    apiRequests, retries);
        }
        ExecutorService executor = Executors.newFixedThreadPool(properties.getConcurrency());
        try {
            for (int start = 0; start < candidates.size(); start += properties.getBatchSize()) {
                int end = Math.min(candidates.size(), start + properties.getBatchSize());
                int currentBatch = start / properties.getBatchSize() + 1;
                BatchOutcome outcome = classifyBatch(candidates.subList(start, end), executor);
                processed += outcome.processed();
                success += outcome.success();
                failed += outcome.failed();
                unknown += outcome.unknown();
                apiRequests += outcome.apiRequests();
                retries += outcome.retries();
                publish(progressConsumer, total, processed, success, failed, skipped, unknown,
                        currentBatch, totalBatches, apiRequests, retries, "성별 분류 Batch 완료");
                log.info("event=gender_classification_batch_completed batch={} batchSize={} processed={} success={} failed={} skipped={} unknown={} apiRequests={} retryCount={}",
                        currentBatch, outcome.processed(), processed, success, failed, skipped, unknown, apiRequests, retries);
            }
        } finally {
            executor.shutdownNow();
        }
        return new GenderClassificationRebuildResult(total, processed, success, failed, skipped, unknown,
                apiRequests, retries);
    }

    PolicyGenderClassificationResult classifyProjection(PolicySearchProjection projection) {
        try {
            return classificationPolicy.normalize(toResult(aiClient.analyze(projection)));
        } catch (RuntimeException ex) {
            return PolicyGenderClassificationResult.unknown("성별 문맥 분류 호출에 실패했습니다.");
        }
    }

    private PolicyGenderClassificationResult classifyProjectionForPersistence(PolicySearchProjection projection) {
        try {
            PolicyGenderAiAnalysis analysis = aiClient.analyze(projection);
            if (analysis == null) {
                return null;
            }
            return classificationPolicy.normalize(toResult(analysis));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private BatchOutcome classifyBatch(List<ClassificationCandidate> batch, ExecutorService executor) {
        List<Future<ClassificationAttempt>> futures = new ArrayList<>();
        for (ClassificationCandidate candidate : batch) {
            futures.add(executor.submit(task(candidate.projection())));
        }
        List<PolicyGenderClassification> updated = new ArrayList<>();
        int processed = 0;
        int success = 0;
        int failed = 0;
        int unknown = 0;
        int apiRequests = 0;
        int retries = 0;
        for (int index = 0; index < futures.size(); index++) {
            ClassificationCandidate candidate = batch.get(index);
            ClassificationAttempt attempt = await(futures.get(index));
            processed++;
            apiRequests += attempt.apiRequests();
            retries += attempt.retries();
            if (attempt.failed()) {
                failed++;
            } else {
                success++;
            }
            if (attempt.result().audience() == PolicyGenderAudience.UNKNOWN) {
                unknown++;
            }
            PolicyGenderClassification classification = candidate.current() == null
                    ? new PolicyGenderClassification(candidate.projection().getPolicy())
                    : candidate.current();
            classification.update(attempt.result(), PolicyGenderClassificationPolicy.VERSION, candidate.sourceHash(),
                    candidate.projection().getUpdatedAt(), LocalDateTime.now());
            updated.add(classification);
        }
        classificationRepository.saveAll(updated);
        return new BatchOutcome(processed, success, failed, unknown, apiRequests, retries);
    }

    private Callable<ClassificationAttempt> task(PolicySearchProjection projection) {
        return () -> {
            int apiRequests = 0;
            int retries = 0;
            for (int attempt = 1; attempt <= properties.getMaxRetries(); attempt++) {
                apiRequests++;
                try {
                    PolicyGenderAiAnalysis analysis = aiClient.analyze(projection);
                    if (analysis == null) {
                        return new ClassificationAttempt(
                                PolicyGenderClassificationResult.unknown("성별 분류 AI 응답이 없습니다."),
                                false,
                                apiRequests,
                                retries
                        );
                    }
                    return new ClassificationAttempt(classificationPolicy.normalize(toResult(analysis)),
                            false, apiRequests, retries);
                } catch (RuntimeException ex) {
                    if (attempt >= properties.getMaxRetries()) {
                        return new ClassificationAttempt(
                                PolicyGenderClassificationResult.unknown("성별 문맥 분류 호출에 실패했습니다."),
                                true,
                                apiRequests,
                                retries
                        );
                    }
                    retries++;
                    backoff(attempt);
                }
            }
            return new ClassificationAttempt(
                    PolicyGenderClassificationResult.unknown("성별 문맥 분류 호출에 실패했습니다."),
                    true,
                    apiRequests,
                    retries
            );
        };
    }

    private ClassificationAttempt await(Future<ClassificationAttempt> future) {
        try {
            return future.get(properties.getCallTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failedAttempt("성별 문맥 분류 작업이 인터럽트되었습니다.");
        } catch (ExecutionException ex) {
            return failedAttempt("성별 문맥 분류 호출에 실패했습니다.");
        } catch (TimeoutException ex) {
            future.cancel(true);
            return failedAttempt("성별 문맥 분류 호출이 시간 초과되었습니다.");
        }
    }

    private ClassificationAttempt failedAttempt(String evidence) {
        return new ClassificationAttempt(PolicyGenderClassificationResult.unknown(evidence), true, 1, 0);
    }

    private void backoff(int attempt) {
        Duration delay = properties.getRetryBackoff().multipliedBy(attempt);
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void publish(Consumer<GenderClassificationProgress> progressConsumer, int total, int processed,
                         int success, int failed, int skipped, int unknown, int currentBatch, int totalBatches,
                         int apiRequests, int retries, String message) {
        if (progressConsumer != null) {
            progressConsumer.accept(new GenderClassificationProgress(total, processed, success, failed, skipped,
                    unknown, currentBatch, totalBatches, apiRequests, retries, message));
        }
    }

    private PolicyGenderClassificationResult toResult(PolicyGenderAiAnalysis analysis) {
        if (analysis == null) {
            return PolicyGenderClassificationResult.unknown("성별 분류 AI 응답이 없습니다.");
        }
        PolicyGenderAudience audience = enumValue(PolicyGenderAudience.class, analysis.audience());
        PolicyApplicantScope scope = enumValue(PolicyApplicantScope.class, analysis.applicantScope());
        if (audience == null || scope == null || analysis.exclusive() == null || analysis.confidence() == null) {
            return PolicyGenderClassificationResult.unknown("성별 분류 AI 응답 값이 유효하지 않습니다.");
        }
        return new PolicyGenderClassificationResult(audience, analysis.exclusive(),
                analysis.confidence(), scope, analysis.evidence());
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private record ClassificationCandidate(PolicySearchProjection projection,
                                           PolicyGenderClassification current,
                                           String sourceHash) {
    }

    private record ClassificationAttempt(PolicyGenderClassificationResult result,
                                         boolean failed,
                                         int apiRequests,
                                         int retries) {
    }

    private record BatchOutcome(int processed,
                                int success,
                                int failed,
                                int unknown,
                                int apiRequests,
                                int retries) {
    }

    public record GenderClassificationProgress(int total,
                                               int processed,
                                               int success,
                                               int failed,
                                               int skipped,
                                               int unknown,
                                               int currentBatch,
                                               int totalBatches,
                                               int apiRequests,
                                               int retries,
                                               String message) {
    }

    public record GenderClassificationRebuildResult(int total,
                                                    int processed,
                                                    int success,
                                                    int failed,
                                                    int skipped,
                                                    int unknown,
                                                    int apiRequests,
                                                    int retries) {
    }
}
