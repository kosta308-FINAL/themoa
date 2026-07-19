package com.weaone.themoa.domain.policy.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PolicySearchProjectionService {
    public static final String VERSION = "policy-search-v2";
    private static final Logger log = LoggerFactory.getLogger(PolicySearchProjectionService.class);

    private final PolicyRepository policyRepository;
    private final PolicySourceSnapshotRepository snapshotRepository;
    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicyKeywordNormalizer normalizer;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder;

    @PersistenceContext
    private EntityManager entityManager;

    public PolicySearchProjectionService(PolicyRepository policyRepository,
                                         PolicySourceSnapshotRepository snapshotRepository,
                                         PolicySearchProjectionRepository projectionRepository,
                                         PolicyKeywordNormalizer normalizer,
                                         ObjectMapper objectMapper,
                                         TransactionTemplate transactionTemplate,
                                         PolicyLexicalIndexBuilder lexicalIndexBuilder) {
        this.policyRepository = policyRepository;
        this.snapshotRepository = snapshotRepository;
        this.projectionRepository = projectionRepository;
        this.normalizer = normalizer;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.lexicalIndexBuilder = lexicalIndexBuilder;
    }

    public ProjectionRebuildResult rebuildAll() {
        return rebuildAll(null);
    }

    public ProjectionRebuildResult rebuildAll(Consumer<ProjectionRebuildProgress> progressConsumer) {
        long total = policyRepository.countByActiveTrue();
        long processed = 0;
        long missingSnapshot = 0;
        int page = 0;
        int batchSize = 200;
        while (true) {
            var ids = policyRepository.findActivePolicyIds(PageRequest.of(page++, batchSize));
            if (ids.isEmpty()) break;
            ProjectionBatchResult result = transactionTemplate.execute(status -> rebuildBatch(ids));
            processed += result.processed();
            missingSnapshot += result.missingSnapshot();
            if (progressConsumer != null) {
                progressConsumer.accept(new ProjectionRebuildProgress(total, processed, missingSnapshot));
            }
        }
        return new ProjectionRebuildResult(total, processed, missingSnapshot);
    }

    @Transactional
    public void rebuildOne(Policy policy) {
        ProjectionSource source = source(policy);
        upsert(policy, source.fields(), source.missingSnapshot());
        lexicalIndexBuilder.invalidate();
    }

    public ProjectionBatchResult rebuildBatch(List<Integer> ids) {
        var policies = policyRepository.findWithRelationsByIdIn(ids);
        Map<Integer, com.weaone.themoa.domain.policy.policy.domain.PolicySourceSnapshot> snapshots =
                snapshotRepository.findByPolicyIdIn(ids).stream()
                        .collect(Collectors.toMap(snapshot -> snapshot.getPolicy().getId(), Function.identity()));
        long missingSnapshot = 0;
        for (Policy policy : policies) {
            ProjectionSource source = source(policy, snapshots.get(policy.getId()));
            upsert(policy, source.fields(), source.missingSnapshot());
            if (source.missingSnapshot()) missingSnapshot++;
        }
        return new ProjectionBatchResult(policies.size(), missingSnapshot);
    }

    private ProjectionSource source(Policy policy) {
        return source(policy, snapshotRepository.findByPolicyId(policy.getId()).orElse(null));
    }

    private ProjectionSource source(Policy policy, com.weaone.themoa.domain.policy.policy.domain.PolicySourceSnapshot snapshot) {
        if (snapshot != null) {
            try {
                Map<String, Object> fields = objectMapper.readValue(snapshot.getRawPolicyJson(), new TypeReference<>() {
                });
                return new ProjectionSource(fields, false);
            } catch (Exception ex) {
                log.warn("Policy snapshot parse failed for search projection. policyId={}", policy.getId(), ex);
            }
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("plcyNm", policy.getTitle());
        fallback.put("plcyExplnCn", policy.getSummary());
        fallback.put("sprvsnInstCdNm", policy.getAgencyName());
        if (policy.getCondition() != null) {
            fallback.put("ptcpPrpTrgtCn", policy.getCondition().getConditionSummary());
        }
        fallback.put("lclsfNm", policy.getCategory() == null ? "" : policy.getCategory().name());
        return new ProjectionSource(fallback, true);
    }

    private void upsert(Policy policy, Map<String, Object> fields, boolean missingSnapshot) {
        var existing = projectionRepository.findByPolicyId(policy.getId());
        String title = mostCompleteTitle(text(fields, "plcyNm", "title", "policyName"),
                policy.getTitle(),
                existing.map(PolicySearchProjection::getTitleText).orElse(null));
        if (!StringUtils.hasText(title)) {
            title = policy.getTitle();
        }
        String keyword = text(fields, "plcyKywdNm", "keyword");
        String category = join(text(fields, "lclsfNm"), text(fields, "mclsfNm"));
        String description = text(fields, "plcyExplnCn");
        String support = text(fields, "plcySprtCn");
        String target = text(fields, "ptcpPrpTrgtCn");
        String qualification = join(text(fields, "addAplyQlfcCndCn"), text(fields, "earnEtcCn"), text(fields, "bizPrdEtcCn"));
        String application = text(fields, "plcyAplyMthdCn");
        String institution = join(text(fields, "sprvsnInstCdNm"), text(fields, "operInstCdNm"),
                text(fields, "rgtrInstCdNm"), text(fields, "rgtrUpInstCdNm"), text(fields, "rgtrHghrkInstCdNm"));
        String full = Stream.of(title, keyword, category, description, support, target, qualification, application, institution)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        PolicySearchProjection projection = existing.orElseGet(() -> new PolicySearchProjection(policy));
        logTitleMismatch(policy, fields, existing.orElse(null), title);
        projection.update(normalizer.normalize(title), title, keyword, category, description, support,
                target, qualification, application, institution, full, VERSION, missingSnapshot);
        if (existing.isEmpty()) {
            entityManager.persist(projection);
        }
    }

    private String text(Map<String, Object> fields, String... keys) {
        for (String key : keys) {
            Object value = fields.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private String join(String... values) {
        return Stream.of(values).filter(StringUtils::hasText).collect(Collectors.joining(" "));
    }

    private String mostCompleteTitle(String... values) {
        String selected = textFrom(values);
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (!StringUtils.hasText(selected) || longerNormalizedPrefix(value, selected)) {
                selected = value.trim();
            }
        }
        return selected;
    }

    private String textFrom(String... values) {
        return Stream.of(values).filter(StringUtils::hasText).map(String::trim).findFirst().orElse("");
    }

    private boolean longerNormalizedPrefix(String candidate, String current) {
        String normalizedCandidate = normalizer.normalize(candidate);
        String normalizedCurrent = normalizer.normalize(current);
        return normalizedCandidate.length() > normalizedCurrent.length()
                && normalizedCandidate.startsWith(normalizedCurrent);
    }

    private void logTitleMismatch(Policy policy, Map<String, Object> fields, PolicySearchProjection existing, String selectedTitle) {
        String sourceTitle = text(fields, "plcyNm", "title", "policyName");
        if (longerNormalizedPrefix(sourceTitle, policy.getTitle())) {
            log.info("TITLE_MISMATCH ENTITY_TITLE_SHORTER_THAN_SOURCE policyId={} selectedTitle={}", policy.getId(), selectedTitle);
        }
        if (existing != null && longerNormalizedPrefix(sourceTitle, existing.getTitleText())) {
            log.info("TITLE_MISMATCH PROJECTION_TITLE_OUTDATED policyId={} selectedTitle={}", policy.getId(), selectedTitle);
        }
        if (StringUtils.hasText(sourceTitle) && longerNormalizedPrefix(policy.getTitle(), sourceTitle)) {
            log.info("TITLE_MISMATCH SOURCE_TITLE_INCOMPLETE policyId={} selectedTitle={}", policy.getId(), selectedTitle);
        }
    }

    private record ProjectionSource(Map<String, Object> fields, boolean missingSnapshot) {
    }

    public record ProjectionRebuildResult(long total, long processed, long missingSnapshot) {
    }

    public record ProjectionBatchResult(long processed, long missingSnapshot) {
    }

    public record ProjectionRebuildProgress(long total, long processed, long missingSnapshot) {
    }
}
