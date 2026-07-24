package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.CandidateSource;
import com.weaone.themoa.domain.policy.rag.dto.PolicyDomainClassification;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

/**
 * 자연어 검색 후보를 수집하고 policyId 기준으로 병합한다.
 *
 * <p>PolicySearchPlanService 이후, PolicyEligibilityEvaluator 이전에 호출된다. 입력은 SearchPlan,
 * SearchIntent, 사용자 지역, page/size이며 출력은 PolicyCandidateCollection과 CandidateEvidence다.</p>
 *
 * <p>DB는 후보 정책 batch 로딩과 지역 적격 pool 조회에 사용하고, 외부 시스템은 Qdrant VectorStore 검색에 사용한다.
 * 이 클래스는 후보 생성만 담당하며 지역/나이/취업/교육 단계 hard filter나 최종 랭킹은 수행하지 않는다.</p>
 *
 * <p>수정 시 sourceRank와 finalRank를 섞지 않아야 한다. source별 실제 rank/rawScore/normalizedScore는
 * Explain에서 그대로 사용되므로 후보 수집 시점의 값을 CandidateSourceEvidence에 보존해야 한다.</p>
 *
 * <p>Vector 재시도는 초기 검색과 다른 evidence다. 같은 CandidateSource에 덮어쓰면 Explain에서 어떤 query,
 * topK, threshold 설정으로 후보가 들어왔는지 사라지므로 INITIAL/RETRY/NO_THRESHOLD를 분리하고,
 * 동일 query와 동일 검색 설정은 한 번만 호출한다.</p>
 */
@Service
public class PolicySearchCandidateRetriever {
    private final PolicyRepository policyRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;
    private final PolicyLexicalSearchService lexicalSearchService;
    private final RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService;
    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicyDomainClassifier fallbackDomainClassifier = new PolicyDomainClassifier();
    private final PolicyKeywordNormalizer normalizer = new PolicyKeywordNormalizer();

    public PolicySearchCandidateRetriever(PolicyRepository policyRepository,
                                          ObjectProvider<VectorStore> vectorStoreProvider,
                                          RagProperties properties,
                                          PolicyLexicalSearchService lexicalSearchService,
                                          RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService,
                                          PolicySearchProjectionRepository projectionRepository) {
        this.policyRepository = policyRepository;
        this.vectorStoreProvider = vectorStoreProvider;
        this.properties = properties;
        this.lexicalSearchService = lexicalSearchService;
        this.regionEligiblePolicyCandidateService = regionEligiblePolicyCandidateService;
        this.projectionRepository = projectionRepository;
    }

    /**
     * SearchPlan과 사용자 지역을 기준으로 후보 정책과 원천 evidence를 수집한다.
     *
     * <p>명시적 제외 의도가 있는 경우 원문 벡터 검색 대신 정규화된 긍정 목적을 사용한다.
     * 부정어가 들어간 원문 embedding이 제외 분야 정책을 다시 끌어오는 것을 막기 위한 기존 규칙이다.</p>
     */
    public PolicyCandidateCollection retrieve(PolicySearchPlan plan,
                                              PolicySearchIntent intent,
                                              ResolvedUserRegion userRegion,
                                              int page,
                                              int size,
                                              int resultSize) {
        PolicySearchCondition condition = plan.condition();
        SearchQueryType queryType = plan.queryType();
        boolean regionPoolApplied = condition.regionExplicit() && regionEligiblePolicyCandidateService != null;
        List<RegionEligiblePolicyCandidate> regionEligibleCandidates = regionPoolApplied
                ? regionEligibleCandidates(plan, userRegion)
                : List.of();
        Set<Integer> regionEligibleIds = regionEligibleCandidates.stream()
                .map(RegionEligiblePolicyCandidate::policyId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        RegionPoolCounts regionPoolCounts = RegionPoolCounts.from(regionEligibleCandidates);

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        boolean mysqlFallbackUsed = false;
        boolean retried = false;
        String fallbackReason = null;
        Map<CandidateSource, List<Document>> vectorCandidatesBySource = new EnumMap<>(CandidateSource.class);
        Map<CandidateSource, String> vectorVariantsBySource = new EnumMap<>(CandidateSource.class);
        Map<VectorSearchKey, List<Document>> vectorCache = new LinkedHashMap<>();

        if (properties.isEnabled() && vectorStore != null) {
            String firstQuery = plan.explicitExclusion() ? plan.normalizedGoal() : plan.originalQuery();
            CandidateSource firstSource = plan.explicitExclusion()
                    ? CandidateSource.VECTOR_NORMALIZED_QUERY
                    : CandidateSource.VECTOR_ORIGINAL_QUERY;
            vectorCandidatesBySource.put(firstSource,
                    vectorSearch(vectorStore, vectorCache, firstQuery, properties.getSearch().getTopK(), true));
            vectorVariantsBySource.put(firstSource, variant(firstQuery, properties.getSearch().getTopK(), true, false));

            String secondQuery = switch (queryType) {
                case POLICY_NAME -> intent.originalQuery();
                case BROAD_DISCOVERY -> categoryQuery(condition, intent);
                case TOPIC_SEARCH, ELIGIBILITY_SEARCH -> intent.semanticQuery();
            };
            if (!secondQuery.equals(plan.originalQuery())) {
                vectorCandidatesBySource.put(CandidateSource.VECTOR_INTENT_INITIAL,
                        vectorSearch(vectorStore, vectorCache, secondQuery, properties.getSearch().getTopK(), true));
                vectorVariantsBySource.put(CandidateSource.VECTOR_INTENT_INITIAL,
                        variant(secondQuery, properties.getSearch().getTopK(), true, false));
            }
            int vectorCandidateCount = vectorCandidatesBySource.values().stream().mapToInt(List::size).sum();
            if (vectorCandidateCount < resultSize) {
                retried = true;
                vectorCandidatesBySource.put(CandidateSource.VECTOR_INTENT_RETRY,
                        vectorSearch(vectorStore, vectorCache, secondQuery, properties.getSearch().getRetryTopK(), true));
                vectorVariantsBySource.put(CandidateSource.VECTOR_INTENT_RETRY,
                        variant(secondQuery, properties.getSearch().getRetryTopK(), true, true));
            }
            if (vectorCandidatesBySource.values().stream().allMatch(List::isEmpty)) {
                retried = true;
                fallbackReason = "VECTOR_THRESHOLD_EMPTY";
                vectorCandidatesBySource.put(CandidateSource.VECTOR_INTENT_NO_THRESHOLD,
                        vectorSearch(vectorStore, vectorCache, secondQuery, properties.getSearch().getRetryTopK(), false));
                vectorVariantsBySource.put(CandidateSource.VECTOR_INTENT_NO_THRESHOLD,
                        variant(secondQuery, properties.getSearch().getRetryTopK(), false, true));
            }
        } else {
            fallbackReason = "VECTOR_SEARCH_DISABLED";
        }

        Map<Integer, Double> semanticScores = semanticScores(vectorCandidatesBySource);
        Map<Integer, Set<CandidateSource>> candidateSources = candidateSources(vectorCandidatesBySource);
        Map<Integer, List<CandidateSourceEvidence>> sourceEvidence = sourceEvidence(vectorCandidatesBySource, vectorVariantsBySource);
        int economicVectorCandidateCount = collectEconomicVectorCandidates(plan, vectorStore, vectorCache, semanticScores,
                candidateSources, sourceEvidence);
        Map<String, Integer> generalVectorCounts = new LinkedHashMap<>();
        int generalVectorCandidateCount = collectGeneralBenefitVectorCandidates(plan, vectorStore, vectorCache, semanticScores,
                candidateSources, sourceEvidence, generalVectorCounts);

        PolicyLexicalSearchService.LexicalSearchResult lexical =
                lexicalSearchService.search(condition, intent, properties.getSearch().getRetryTopK());
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>(lexical.lexicalScores());
        Map<Integer, Double> titleExactScores = new LinkedHashMap<>(lexical.titleExactScores());
        for (int i = 0; i < lexical.policyIds().size(); i++) {
            Integer policyId = lexical.policyIds().get(i);
            int rank = i + 1;
            double lexicalScore = lexicalScores.getOrDefault(policyId, 0.0);
            double titleScore = titleExactScores.getOrDefault(policyId, 0.0);
            Set<CandidateSource> merged = candidateSources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class));
            Set<CandidateSource> lexicalSources = lexical.candidateSources().getOrDefault(policyId, Set.of());
            merged.addAll(lexicalSources);
            merged.add(CandidateSource.LEXICAL_INDEX);
            addEvidence(sourceEvidence, policyId, CandidateSource.LEXICAL_INDEX, rank, lexicalScore, lexicalScore, "BM25");
            for (CandidateSource source : lexicalSources) {
                addEvidence(sourceEvidence, policyId, source, rank, lexicalScore, lexicalScore, "BM25_FIELD");
            }
            if (titleScore >= 1.0) {
                merged.add(CandidateSource.EXACT_TITLE);
                addEvidence(sourceEvidence, policyId, CandidateSource.EXACT_TITLE, rank, titleScore, titleScore, "TITLE");
            } else if (titleScore >= 0.75) {
                merged.add(CandidateSource.TITLE_PHRASE);
                addEvidence(sourceEvidence, policyId, CandidateSource.TITLE_PHRASE, rank, titleScore, titleScore, "TITLE");
            }
        }
        int economicLexicalCandidateCount = collectEconomicLexicalCandidates(plan, condition, lexicalScores, titleExactScores,
                candidateSources, sourceEvidence);
        Map<String, Integer> generalLexicalCounts = new LinkedHashMap<>();
        int generalLexicalCandidateCount = collectGeneralBenefitLexicalCandidates(plan, condition, lexicalScores, titleExactScores,
                candidateSources, sourceEvidence, generalLexicalCounts);
        LinkedHashSet<Integer> exactTitleIds = titleExactScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= 1.0)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean exactTitleSuppressed = queryType == SearchQueryType.POLICY_NAME
                && !exactTitleIds.isEmpty()
                && !relatedTitleSearchRequested(plan.originalQuery());
        boolean nearTitleSearch = queryType == SearchQueryType.POLICY_NAME
                && exactTitleIds.isEmpty()
                && titleExactScores.values().stream().anyMatch(score -> score >= 0.75);

        LinkedHashSet<Integer> mergedIds = new LinkedHashSet<>(semanticScores.keySet());
        int beforeLexicalMerge = mergedIds.size();
        mergedIds.addAll(lexical.policyIds());
        mergedIds.addAll(lexicalScores.keySet());
        if (exactTitleSuppressed) {
            mergedIds.clear();
            mergedIds.addAll(exactTitleIds);
        }
        int initialMergedCandidateCount = mergedIds.size();
        if (regionPoolApplied) {
            mergedIds.removeIf(id -> !regionEligibleIds.contains(id));
        }
        int regionIntersectionCandidateCount = mergedIds.size();
        int broadFallbackAddedCount = 0;
        if (!exactTitleSuppressed && shouldProtectRegionPool(plan)) {
            Set<Integer> localRegionEligibleIds = localRegionEligibleIds(regionEligibleCandidates);
            if (!localRegionEligibleIds.isEmpty()) {
                List<FallbackCandidate> protectedRegionCandidates = broadFallbackCandidates(plan, intent,
                        true, localRegionEligibleIds, mergedIds, fallbackTarget(resultSize));
                protectedRegionCandidates.forEach(candidate -> {
                    Integer id = candidate.policyId();
                    candidateSources.computeIfAbsent(id, ignored -> EnumSet.noneOf(CandidateSource.class))
                            .add(CandidateSource.BROAD_FALLBACK);
                    semanticScores.merge(id, candidate.score(), Math::max);
                    lexicalScores.merge(id, candidate.score(), Math::max);
                    addEvidence(sourceEvidence, id, CandidateSource.BROAD_FALLBACK, null,
                            candidate.score(), candidate.score(),
                            "REGION_PRIORITY_POOL terms=" + String.join("/", candidate.matchedTerms()));
                });
                mergedIds.addAll(protectedRegionCandidates.stream().map(FallbackCandidate::policyId).toList());
                broadFallbackAddedCount += protectedRegionCandidates.size();
            }
        }
        if (!exactTitleSuppressed
                && (queryType == SearchQueryType.BROAD_DISCOVERY || queryType == SearchQueryType.ELIGIBILITY_SEARCH)
                && shouldUseBroadFallback(mergedIds.size(), resultSize)
                && properties.getSearch().isMysqlFallbackEnabled()) {
            int target = fallbackTarget(resultSize);
            List<FallbackCandidate> broadCandidates = broadFallbackCandidates(plan, intent, regionPoolApplied, regionEligibleIds, mergedIds,
                    Math.max(0, target - mergedIds.size()));
            broadCandidates.forEach(candidate -> {
                Integer id = candidate.policyId();
                candidateSources.computeIfAbsent(id, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.BROAD_FALLBACK);
                semanticScores.merge(id, candidate.score(), Math::max);
                lexicalScores.merge(id, candidate.score(), Math::max);
                addEvidence(sourceEvidence, id, CandidateSource.BROAD_FALLBACK, null,
                        candidate.score(), candidate.score(), "BROAD_FALLBACK_REGION_POOL terms=" + String.join("/", candidate.matchedTerms()));
            });
            mergedIds.addAll(broadCandidates.stream().map(FallbackCandidate::policyId).toList());
            broadFallbackAddedCount += broadCandidates.size();
        }

        int rawCandidateCount = vectorCandidatesBySource.values().stream().mapToInt(List::size).sum()
                + lexical.policyIds().size()
                + economicVectorCandidateCount
                + economicLexicalCandidateCount
                + generalVectorCandidateCount
                + generalLexicalCandidateCount
                + broadFallbackAddedCount;
        int duplicateCandidateCount = Math.max(0, rawCandidateCount - mergedIds.size());
        List<Policy> policies = mergedIds.isEmpty()
                ? List.of()
                : policyRepository.findWithRelationsByIdIn(new ArrayList<>(mergedIds));
        if (exactTitleSuppressed) {
            policies = deduplicateExactTitlePolicies(policies, exactTitleIds);
        }
        if (!exactTitleSuppressed && policies.isEmpty() && properties.getSearch().isMysqlFallbackEnabled()) {
            mysqlFallbackUsed = true;
            fallbackReason = fallbackReason == null ? "NO_CANDIDATES" : fallbackReason;
            List<Integer> fallbackIds = broadFallbackCandidates(plan, intent, regionPoolApplied, regionEligibleIds, mergedIds,
                    fallbackTarget(resultSize)).stream().map(FallbackCandidate::policyId).toList();
            if (fallbackIds.isEmpty() && !regionPoolApplied) {
                fallbackIds = policyRepository.findActivePolicyIds(PageRequest.of(0, properties.getSearch().getRetryTopK()));
            }
            policies = policyRepository.findWithRelationsByIdIn(fallbackIds);
            fallbackIds.forEach(id -> {
                candidateSources.computeIfAbsent(id, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.MYSQL_FALLBACK);
                addEvidence(sourceEvidence, id, CandidateSource.MYSQL_FALLBACK, null, null, null, "MYSQL_FALLBACK");
            });
        }

        int fallbackAddedCount = broadFallbackAddedCount + (mysqlFallbackUsed ? Math.max(0, policies.size() - beforeLexicalMerge) : 0);
        CandidateCollectionMetrics metrics = new CandidateCollectionMetrics(
                vectorCandidatesBySource.values().stream().mapToInt(List::size).sum(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_ORIGINAL_QUERY, List.of()).size(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_INTENT_QUERY, List.of()).size()
                        + vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_INTENT_INITIAL, List.of()).size()
                        + vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_INTENT_RETRY, List.of()).size()
                        + vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_INTENT_NO_THRESHOLD, List.of()).size(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_EXPANDED_QUERY, List.of()).size()
                        + economicVectorCandidateCount,
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_CATEGORY_QUERY, List.of()).size()
                        + generalVectorCandidateCount,
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_NORMALIZED_QUERY, List.of()).size(),
                lexical.policyIds().size() + economicLexicalCandidateCount,
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_TITLE, 0),
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_KEYWORD, 0),
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_SUMMARY, 0),
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_CATEGORY, 0),
                policies.size(),
                duplicateCandidateCount,
                fallbackAddedCount,
                regionPoolCounts.total(),
                regionPoolCounts.exactSigungu(),
                regionPoolCounts.parentSido(),
                regionPoolCounts.nationwide(),
                regionPoolCounts.multiple(),
                initialMergedCandidateCount,
                regionIntersectionCandidateCount,
                false,
                0,
                0,
                exactTitleSuppressed ? "EXACT_TITLE" : (nearTitleSearch ? "NEAR_TITLE" : "NORMAL_SEARCH"),
                normalizer.normalize(plan.originalQuery()),
                matchedTitle(policies, exactTitleIds),
                titleExactScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0),
                exactTitleIds.size(),
                exactTitleSuppressed,
                generalBenefitSubQueries(plan),
                countText(generalVectorCounts),
                countText(generalLexicalCounts)
        );
        Map<Integer, CandidateEvidence> evidenceByPolicyId = new LinkedHashMap<>();
        candidateSources.keySet().forEach(policyId -> evidenceByPolicyId.put(policyId,
                new CandidateEvidence(policyId,
                        sourceEvidence.getOrDefault(policyId, List.of()),
                        rawSemanticScore(sourceEvidence.getOrDefault(policyId, List.of())),
                        semanticScores.getOrDefault(policyId, 0.0),
                        semanticScores.getOrDefault(policyId, 0.0) * 0.35,
                        semanticScores.getOrDefault(policyId, 0.0),
                        lexicalScores.getOrDefault(policyId, 0.0),
                        titleExactScores.getOrDefault(policyId, 0.0))));
        return new PolicyCandidateCollection(policies, evidenceByPolicyId, semanticScores, lexicalScores,
                titleExactScores, candidateSources, metrics, retried, mysqlFallbackUsed, fallbackReason);
    }

    private boolean relatedTitleSearchRequested(String query) {
        if (!StringUtils.hasText(query)) {
            return false;
        }
        String compact = query.replaceAll("\\s+", "");
        return containsAny(compact, "비슷한", "유사한", "관련", "같은", "교통정책도", "정책도알려");
    }

    private List<RegionEligiblePolicyCandidate> regionEligibleCandidates(PolicySearchPlan plan,
                                                                         ResolvedUserRegion userRegion) {
        if (plan.queryType() == SearchQueryType.POLICY_NAME) {
            return regionEligiblePolicyCandidateService.findEligibleCandidates(userRegion);
        }
        return regionEligiblePolicyCandidateService.findSearchEligibleCandidates(userRegion);
    }

    private boolean shouldProtectRegionPool(PolicySearchPlan plan) {
        return plan.condition().regionExplicit()
                && plan.queryType() != SearchQueryType.POLICY_NAME
                && properties.getSearch().isMysqlFallbackEnabled();
    }

    private Set<Integer> localRegionEligibleIds(List<RegionEligiblePolicyCandidate> candidates) {
        return candidates.stream()
                .filter(candidate -> candidate.compatibility().priority() < RegionCompatibility.NATIONWIDE.priority())
                .map(RegionEligiblePolicyCandidate::policyId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Policy> deduplicateExactTitlePolicies(List<Policy> policies, Set<Integer> exactTitleIds) {
        Map<String, Policy> selected = new LinkedHashMap<>();
        for (Policy policy : policies) {
            if (!exactTitleIds.contains(policy.getId())) {
                continue;
            }
            selected.putIfAbsent(normalizer.normalize(normalizer.titleIdentity(policy.getTitle()).canonicalTitle()), policy);
        }
        return new ArrayList<>(selected.values());
    }

    private String matchedTitle(List<Policy> policies, Set<Integer> exactTitleIds) {
        return policies.stream()
                .filter(policy -> exactTitleIds.contains(policy.getId()))
                .map(Policy::getTitle)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }

    public PolicyCandidateCollection retrieveBroadFallback(PolicySearchPlan plan,
                                                           PolicySearchIntent intent,
                                                           ResolvedUserRegion userRegion,
                                                           Set<Integer> alreadySelected,
                                                           int limit) {
        boolean regionPoolApplied = plan.condition().regionExplicit() && regionEligiblePolicyCandidateService != null;
        List<RegionEligiblePolicyCandidate> regionEligibleCandidates = regionPoolApplied
                ? regionEligibleCandidates(plan, userRegion)
                : List.of();
        Set<Integer> regionEligibleIds = regionEligibleCandidates.stream()
                .map(RegionEligiblePolicyCandidate::policyId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        RegionPoolCounts regionPoolCounts = RegionPoolCounts.from(regionEligibleCandidates);
        List<FallbackCandidate> fallbackCandidates = broadFallbackCandidates(plan, intent, regionPoolApplied,
                regionEligibleIds, alreadySelected == null ? Set.of() : alreadySelected, limit);
        List<Integer> ids = fallbackCandidates.stream().map(FallbackCandidate::policyId).toList();
        List<Policy> policies = ids.isEmpty() ? List.of() : policyRepository.findWithRelationsByIdIn(ids);
        Map<Integer, Double> semanticScores = new LinkedHashMap<>();
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>();
        Map<Integer, Double> titleExactScores = new LinkedHashMap<>();
        Map<Integer, Set<CandidateSource>> candidateSources = new LinkedHashMap<>();
        Map<Integer, List<CandidateSourceEvidence>> sourceEvidence = new LinkedHashMap<>();
        for (int i = 0; i < fallbackCandidates.size(); i++) {
            FallbackCandidate candidate = fallbackCandidates.get(i);
            Integer id = candidate.policyId();
            semanticScores.put(id, candidate.score());
            lexicalScores.put(id, candidate.score());
            titleExactScores.put(id, 0.0);
            candidateSources.put(id, EnumSet.of(CandidateSource.BROAD_FALLBACK));
            addEvidence(sourceEvidence, id, CandidateSource.BROAD_FALLBACK, i + 1,
                    candidate.score(), candidate.score(),
                    "BROAD_FALLBACK_POST_RANKING terms=" + String.join("/", candidate.matchedTerms()));
        }
        Map<Integer, CandidateEvidence> evidenceByPolicyId = new LinkedHashMap<>();
        candidateSources.keySet().forEach(policyId -> evidenceByPolicyId.put(policyId,
                new CandidateEvidence(policyId,
                        sourceEvidence.getOrDefault(policyId, List.of()),
                        rawSemanticScore(sourceEvidence.getOrDefault(policyId, List.of())),
                        semanticScores.getOrDefault(policyId, 0.0),
                        semanticScores.getOrDefault(policyId, 0.0) * 0.35,
                        semanticScores.getOrDefault(policyId, 0.0),
                        lexicalScores.getOrDefault(policyId, 0.0),
                        titleExactScores.getOrDefault(policyId, 0.0))));
        CandidateCollectionMetrics metrics = new CandidateCollectionMetrics(0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, policies.size(), 0, fallbackCandidates.size(),
                regionPoolCounts.total(), regionPoolCounts.exactSigungu(), regionPoolCounts.parentSido(),
                regionPoolCounts.nationwide(), regionPoolCounts.multiple());
        return new PolicyCandidateCollection(policies, evidenceByPolicyId, semanticScores, lexicalScores,
                titleExactScores, candidateSources, metrics, false, false, fallbackCandidates.isEmpty() ? "BROAD_FALLBACK_EMPTY" : null);
    }

    private List<Document> vectorSearch(VectorStore vectorStore, Map<VectorSearchKey, List<Document>> cache,
                                        String query, int topK, boolean applyThreshold) {
        VectorSearchKey key = new VectorSearchKey(query, topK, applyThreshold);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK);
        if (applyThreshold) {
            builder.similarityThreshold(properties.getSearch().getMinimumSimilarity());
        }
        List<Document> result = vectorStore.similaritySearch(builder.build());
        cache.put(key, result);
        return result;
    }

    private Map<Integer, Double> semanticScores(Map<CandidateSource, List<Document>> documentsBySource) {
        Map<Integer, Double> scores = new LinkedHashMap<>();
        for (Map.Entry<CandidateSource, List<Document>> entry : documentsBySource.entrySet()) {
            double weight = switch (entry.getKey()) {
                case VECTOR_ORIGINAL_QUERY -> 1.0;
                case VECTOR_NORMALIZED_QUERY -> 0.95;
                case VECTOR_INTENT_QUERY, VECTOR_INTENT_INITIAL -> 0.85;
                case VECTOR_INTENT_RETRY -> 0.8;
                case VECTOR_INTENT_NO_THRESHOLD -> 0.75;
                default -> 0.6;
            };
            List<Document> documents = entry.getValue();
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                Integer policyId = policyId(document);
                if (policyId != null) {
                    double vector = document.getScore() == null ? 0.0 : document.getScore();
                    double rrf = weight / (60.0 + i + 1) * 20.0;
                    scores.merge(policyId, Math.min(1.0, Math.max(vector, rrf)), Math::max);
                }
            }
        }
        return scores;
    }

    private double rawSemanticScore(List<CandidateSourceEvidence> evidence) {
        return evidence.stream()
                .filter(item -> isSemanticSource(item.source()))
                .map(CandidateSourceEvidence::rawScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    private boolean isSemanticSource(CandidateSource source) {
        return switch (source) {
            case VECTOR_ORIGINAL_QUERY, VECTOR_NORMALIZED_QUERY, VECTOR_INTENT_QUERY, VECTOR_INTENT_INITIAL,
                    VECTOR_INTENT_RETRY, VECTOR_INTENT_NO_THRESHOLD, VECTOR_EXPANDED_QUERY, VECTOR_CATEGORY_QUERY,
                    BROAD_FALLBACK -> true;
            default -> false;
        };
    }

    private Map<Integer, Set<CandidateSource>> candidateSources(Map<CandidateSource, List<Document>> documentsBySource) {
        Map<Integer, Set<CandidateSource>> sources = new LinkedHashMap<>();
        for (Map.Entry<CandidateSource, List<Document>> entry : documentsBySource.entrySet()) {
            for (Document document : entry.getValue()) {
                Integer policyId = policyId(document);
                if (policyId != null) {
                    sources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class)).add(entry.getKey());
                }
            }
        }
        return sources;
    }

    private Map<Integer, List<CandidateSourceEvidence>> sourceEvidence(Map<CandidateSource, List<Document>> documentsBySource,
                                                                       Map<CandidateSource, String> variantsBySource) {
        Map<Integer, List<CandidateSourceEvidence>> evidence = new LinkedHashMap<>();
        for (Map.Entry<CandidateSource, List<Document>> entry : documentsBySource.entrySet()) {
            List<Document> documents = entry.getValue();
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                Integer policyId = policyId(document);
                if (policyId != null) {
                    double score = document.getScore() == null ? 0.0 : document.getScore();
                    addEvidence(evidence, policyId, entry.getKey(), i + 1, score, score,
                            variantsBySource.getOrDefault(entry.getKey(), entry.getKey().name()));
                }
            }
        }
        return evidence;
    }

    private String variant(String query, int topK, boolean thresholdApplied, boolean retry) {
        return "query=" + query + ";topK=" + topK + ";thresholdApplied=" + thresholdApplied + ";retry=" + retry;
    }

    private void addEvidence(Map<Integer, List<CandidateSourceEvidence>> evidence,
                             Integer policyId,
                             CandidateSource source,
                             Integer rank,
                             Double rawScore,
                             Double normalizedScore,
                             String queryVariant) {
        evidence.computeIfAbsent(policyId, ignored -> new ArrayList<>())
                .add(new CandidateSourceEvidence(source, rank, rawScore, normalizedScore, queryVariant));
    }

    private Integer policyId(Document document) {
        Object value = document.getMetadata().get("policyId");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Integer.parseInt(text);
        }
        return null;
    }

    private String categoryQuery(PolicySearchCondition condition, PolicySearchIntent intent) {
        if (StringUtils.hasText(condition.category())) {
            return "청년 " + condition.category() + " 지원 정책";
        }
        return intent.intentTerms().isEmpty() ? intent.semanticQuery() : String.join(" ", intent.intentTerms()) + " 정책";
    }

    private int collectEconomicVectorCandidates(PolicySearchPlan plan,
                                                VectorStore vectorStore,
                                                Map<VectorSearchKey, List<Document>> vectorCache,
                                                Map<Integer, Double> semanticScores,
                                                Map<Integer, Set<CandidateSource>> candidateSources,
                                                Map<Integer, List<CandidateSourceEvidence>> sourceEvidence) {
        if (vectorStore == null || !plan.benefitGroups().contains(BenefitGroup.ECONOMIC_SUPPORT)) {
            return 0;
        }
        int total = 0;
        for (EconomicSubQuery query : economicSubQueries(plan)) {
            List<Document> docs = vectorSearch(vectorStore, vectorCache, query.query(), Math.min(30, properties.getSearch().getTopK()), true);
            total += docs.size();
            for (int i = 0; i < docs.size(); i++) {
                Document doc = docs.get(i);
                Integer policyId = policyId(doc);
                if (policyId == null) continue;
                double vector = doc.getScore() == null ? 0.0 : doc.getScore();
                double score = Math.min(1.0, Math.max(vector * query.weight(), rrf(i + 1, query.weight())));
                semanticScores.merge(policyId, score, Math::max);
                candidateSources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.VECTOR_EXPANDED_QUERY);
                addEvidence(sourceEvidence, policyId, CandidateSource.VECTOR_EXPANDED_QUERY, i + 1, vector, score,
                        query.name() + ";query=" + query.query());
            }
        }
        return total;
    }

    private int collectEconomicLexicalCandidates(PolicySearchPlan plan,
                                                 PolicySearchCondition condition,
                                                 Map<Integer, Double> lexicalScores,
                                                 Map<Integer, Double> titleExactScores,
                                                 Map<Integer, Set<CandidateSource>> candidateSources,
                                                 Map<Integer, List<CandidateSourceEvidence>> sourceEvidence) {
        if (!plan.benefitGroups().contains(BenefitGroup.ECONOMIC_SUPPORT)) {
            return 0;
        }
        int total = 0;
        for (EconomicSubQuery query : economicSubQueries(plan)) {
            PolicySearchIntent subIntent = new PolicySearchIntent(plan.originalQuery(), Set.of(), Set.of(query.label()),
                    Set.copyOf(query.terms()), query.query(), String.join(" ", query.terms()));
            PolicyLexicalSearchService.LexicalSearchResult result =
                    lexicalSearchService.search(condition, subIntent, Math.min(50, properties.getSearch().getRetryTopK()));
            total += result.policyIds().size();
            for (int i = 0; i < result.policyIds().size(); i++) {
                Integer policyId = result.policyIds().get(i);
                double raw = result.lexicalScores().getOrDefault(policyId, 0.0);
                double score = Math.max(raw, rrf(i + 1, query.weight()));
                lexicalScores.merge(policyId, score, Math::max);
                titleExactScores.merge(policyId, result.titleExactScores().getOrDefault(policyId, 0.0), Math::max);
                candidateSources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.LEXICAL_INDEX);
                addEvidence(sourceEvidence, policyId, CandidateSource.LEXICAL_INDEX, i + 1, raw, score,
                        query.name() + ";LEXICAL;query=" + query.query());
            }
        }
        return total;
    }

    private int collectGeneralBenefitVectorCandidates(PolicySearchPlan plan,
                                                      VectorStore vectorStore,
                                                      Map<VectorSearchKey, List<Document>> vectorCache,
                                                      Map<Integer, Double> semanticScores,
                                                      Map<Integer, Set<CandidateSource>> candidateSources,
                                                      Map<Integer, List<CandidateSourceEvidence>> sourceEvidence,
                                                      Map<String, Integer> counts) {
        if (vectorStore == null || !plan.benefitGroups().contains(BenefitGroup.GENERAL_BENEFIT)) {
            return 0;
        }
        int total = 0;
        for (GeneralBenefitSubQuery query : generalBenefitSubQueries()) {
            List<Document> docs = vectorSearch(vectorStore, vectorCache, query.query(), Math.min(20, properties.getSearch().getTopK()), true);
            counts.put(query.name(), docs.size());
            total += docs.size();
            for (int i = 0; i < docs.size(); i++) {
                Document doc = docs.get(i);
                Integer policyId = policyId(doc);
                if (policyId == null) continue;
                double vector = doc.getScore() == null ? 0.0 : doc.getScore();
                double score = Math.min(1.0, Math.max(vector * query.weight(), rrf(i + 1, query.weight())));
                semanticScores.merge(policyId, score, Math::max);
                candidateSources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.VECTOR_CATEGORY_QUERY);
                addEvidence(sourceEvidence, policyId, CandidateSource.VECTOR_CATEGORY_QUERY, i + 1, vector, score,
                        query.name() + ";GENERAL_BENEFIT;query=" + query.query());
            }
        }
        return total;
    }

    private int collectGeneralBenefitLexicalCandidates(PolicySearchPlan plan,
                                                       PolicySearchCondition condition,
                                                       Map<Integer, Double> lexicalScores,
                                                       Map<Integer, Double> titleExactScores,
                                                       Map<Integer, Set<CandidateSource>> candidateSources,
                                                       Map<Integer, List<CandidateSourceEvidence>> sourceEvidence,
                                                       Map<String, Integer> counts) {
        if (!plan.benefitGroups().contains(BenefitGroup.GENERAL_BENEFIT)) {
            return 0;
        }
        int total = 0;
        for (GeneralBenefitSubQuery query : generalBenefitSubQueries()) {
            PolicySearchIntent subIntent = new PolicySearchIntent(plan.originalQuery(), Set.of(), Set.of(query.label()),
                    Set.copyOf(query.terms()), query.query(), String.join(" ", query.terms()));
            PolicyLexicalSearchService.LexicalSearchResult result = lexicalSearchService.search(condition, subIntent, 20);
            counts.put(query.name(), result.policyIds().size());
            total += result.policyIds().size();
            for (int i = 0; i < result.policyIds().size(); i++) {
                Integer policyId = result.policyIds().get(i);
                double raw = result.lexicalScores().getOrDefault(policyId, 0.0);
                double score = Math.max(raw, rrf(i + 1, query.weight()));
                lexicalScores.merge(policyId, score, Math::max);
                titleExactScores.merge(policyId, result.titleExactScores().getOrDefault(policyId, 0.0), Math::max);
                candidateSources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.LEXICAL_INDEX);
                addEvidence(sourceEvidence, policyId, CandidateSource.LEXICAL_INDEX, i + 1, raw, score,
                        query.name() + ";GENERAL_BENEFIT_LEXICAL;query=" + query.query());
            }
        }
        return total;
    }

    private boolean shouldUseBroadFallback(int currentSize, int resultSize) {
        int target = fallbackTarget(resultSize);
        return currentSize < target;
    }

    private int fallbackTarget(int resultSize) {
        return Math.max(5, Math.min(12, Math.max(1, resultSize)));
    }

    private List<FallbackCandidate> broadFallbackCandidates(PolicySearchPlan plan,
                                                            PolicySearchIntent intent,
                                                            boolean regionPoolApplied,
                                                            Set<Integer> regionEligibleIds,
                                                            Set<Integer> alreadySelected,
                                                            int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Set<Integer> allowedIds = regionPoolApplied ? regionEligibleIds : Set.of();
        List<FallbackCandidate> projectionMatches = projectionRepository == null ? List.of()
                : projectionRepository.findAllActive().stream()
                .filter(projection -> !alreadySelected.contains(projection.getPolicyId()))
                .filter(projection -> !regionPoolApplied || allowedIds.contains(projection.getPolicyId()))
                .map(projection -> fallbackCandidate(projection, plan, intent))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(FallbackCandidate::score).reversed()
                        .thenComparing(FallbackCandidate::policyId))
                .limit(limit)
                .toList();
        if (!projectionMatches.isEmpty()) {
            return projectionMatches;
        }
        List<Integer> candidateIds = regionPoolApplied
                ? regionEligibleIds.stream().filter(id -> !alreadySelected.contains(id)).toList()
                : policyRepository.findActivePolicyIds(PageRequest.of(0, Math.max(properties.getSearch().getRetryTopK(), limit * 6)));
        return policyRepository.findWithRelationsByIdIn(candidateIds).stream()
                .filter(policy -> policy.isActive())
                .filter(policy -> !alreadySelected.contains(policy.getId()))
                .map(policy -> fallbackCandidate(policy, plan, intent))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(FallbackCandidate::score).reversed()
                        .thenComparing(FallbackCandidate::policyId))
                .limit(limit)
                .toList();
    }

    private FallbackCandidate fallbackCandidate(PolicySearchProjection projection, PolicySearchPlan plan, PolicySearchIntent intent) {
        String text = String.join(" ", nullToEmpty(projection.getTitleText()), nullToEmpty(projection.getKeywordText()),
                nullToEmpty(projection.getCategoryText()), nullToEmpty(projection.getSupportText()),
                nullToEmpty(projection.getTargetText()), nullToEmpty(projection.getQualificationText()),
                nullToEmpty(projection.getDescriptionText()), nullToEmpty(projection.getFullSearchText()));
        ScoreTerms score = fallbackScore(text, plan, intent);
        return new FallbackCandidate(projection.getPolicyId(), score.score(), score.terms());
    }

    private FallbackCandidate fallbackCandidate(Policy policy, PolicySearchPlan plan, PolicySearchIntent intent) {
        PolicyDomainClassification domain = fallbackDomainClassifier.classify(policy);
        if (!plan.desiredDomains().isEmpty() && plan.desiredDomains().stream().noneMatch(domain.primaryDomain()::equals)
                && domain.secondaryDomains().stream().noneMatch(plan.desiredDomains()::contains)
                && !plan.desiredDomains().contains(SearchDomain.GENERAL)) {
            return new FallbackCandidate(policy.getId(), 0.0, List.of());
        }
        ScoreTerms score = fallbackScore(policyText(policy), plan, intent);
        if (plan.benefitGroups().contains(BenefitGroup.ECONOMIC_SUPPORT)
                && domain.supportIntents().stream().anyMatch(this::economicSupport)) {
            score = score.plus(0.25, "policySupportIntent");
        }
        if (plan.benefitGroups().contains(BenefitGroup.GENERAL_BENEFIT)
                && Set.of(SearchDomain.GENERAL, SearchDomain.WELFARE, SearchDomain.CARE, SearchDomain.HEALTH,
                SearchDomain.CULTURE, SearchDomain.HOUSING, SearchDomain.FINANCE).contains(domain.primaryDomain())) {
            score = score.plus(0.2, "generalDomain");
        }
        return new FallbackCandidate(policy.getId(), score.score(), score.terms());
    }

    private ScoreTerms fallbackScore(String rawText, PolicySearchPlan plan, PolicySearchIntent intent) {
        String text = rawText == null ? "" : rawText;
        double score = 0.0;
        List<String> terms = new ArrayList<>();
        if (plan.benefitGroups().contains(BenefitGroup.ECONOMIC_SUPPORT)) {
            for (EconomicSubQuery query : economicSubQueries(plan)) {
                int matches = 0;
                for (String term : query.terms()) {
                    if (text.contains(term)) {
                        matches++;
                        terms.add(query.name() + ":" + term);
                    }
                }
                if (matches > 0) {
                    score = Math.max(score, Math.min(1.0, 0.25 + matches * 0.12) * query.weight());
                }
            }
        }
        if (plan.benefitGroups().contains(BenefitGroup.GENERAL_BENEFIT)) {
            for (String term : List.of("복지", "생활 지원", "생활지원", "주거", "교통", "문화", "건강", "재직자", "근로 청년", "혜택")) {
                if (text.contains(term)) {
                    score = Math.max(score, 0.45);
                    terms.add("GENERAL:" + term);
                }
            }
        }
        for (String term : intent.intentTerms()) {
            if (StringUtils.hasText(term) && text.contains(term)) {
                score = Math.max(score, 0.35);
                terms.add("intent:" + term);
            }
        }
        return new ScoreTerms(Math.min(1.0, score), terms.stream().distinct().toList());
    }

    private boolean economicSupport(SupportIntent intent) {
        return switch (intent) {
            case CASH_ASSISTANCE, ALLOWANCE, LOAN, SAVINGS, MATCHED_SAVINGS, ASSET_BUILDING, HOUSING_COST -> true;
            default -> false;
        };
    }

    private List<EconomicSubQuery> economicSubQueries(PolicySearchPlan plan) {
        if (!plan.benefitGroups().contains(BenefitGroup.ECONOMIC_SUPPORT)) {
            return List.of();
        }
        String query = plan.originalQuery() == null ? "" : plan.originalQuery();
        return List.of(
                new EconomicSubQuery("ECONOMIC_CASH", "CASH", "청년 지원금 수당 보조금 장려금 생활비 활동비",
                        List.of("지원금", "수당", "보조금", "장려금", "생활비", "활동비"), priority(query, 1.0, "현금만", "현금으로직접", "직접현금", "현금지급만")),
                new EconomicSubQuery("ECONOMIC_SAVINGS", "SAVINGS", "청년 저축 계좌 통장 자산형성 정부기여금 매칭 적립",
                        List.of("저축", "계좌", "통장", "자산형성", "정부기여금", "정부기여", "매칭", "적립"), priority(query, 0.9, "통장", "저축", "계좌", "적금", "자산형성", "정부기여", "매칭")),
                new EconomicSubQuery("ECONOMIC_FINANCE", "FINANCE", "청년 대출 융자 이자 지원 보증 금융 부담 완화",
                        List.of("대출", "융자", "이자", "보증", "금융", "부담 완화", "부담완화"), priority(query, 0.85, "대출", "융자", "이자", "보증", "금융")),
                new EconomicSubQuery("ECONOMIC_LIVING_COST", "LIVING_COST", "청년 월세 주거비 교통비 환급 할인 바우처 비용 지원",
                        List.of("월세", "주거비", "교통비", "대중교통", "환급", "페이백", "할인", "바우처", "비용 지원", "비용지원", "비용 경감", "이용료"), priority(query, 0.9, "월세", "주거비", "교통비", "K-패스", "케이패스", "환급", "할인", "바우처"))
        );
    }

    private List<GeneralBenefitSubQuery> generalBenefitSubQueries() {
        return List.of(
                new GeneralBenefitSubQuery("LIVING_TRANSPORT", "TRANSPORT",
                        "청년 대중교통 교통비 환급 통근 이동 생활비 부담 완화",
                        List.of("대중교통", "교통비", "환급", "통근", "출근", "이동", "생활비", "부담 완화", "부담완화", "K-패스", "K패스"), 1.05),
                new GeneralBenefitSubQuery("HOUSING", "HOUSING",
                        "청년 주거 월세 전세 임대 주택 주거비 지원",
                        List.of("주거", "월세", "전세", "임대", "주택", "주거비"), 0.92),
                new GeneralBenefitSubQuery("ECONOMIC_ASSET", "ASSET",
                        "청년 자산형성 저축 통장 계좌 정부기여금 생활 안정",
                        List.of("자산형성", "저축", "통장", "계좌", "정부기여금", "생활 안정", "생활안정"), 0.9),
                new GeneralBenefitSubQuery("CULTURE_WELFARE", "CULTURE_WELFARE",
                        "청년 문화 예술 관람 복지 건강 상담 여가 생활 지원",
                        List.of("문화", "예술", "관람", "복지", "건강", "상담", "여가", "생활 지원", "생활지원"), 0.95),
                new GeneralBenefitSubQuery("EMPLOYEE_LIFE", "EMPLOYEE_LIFE",
                        "재직 청년 근로자 생활 복지 교육 역량개발",
                        List.of("재직 청년", "재직청년", "근로자", "생활 복지", "생활복지", "교육", "역량개발"), 0.88)
        );
    }

    private String generalBenefitSubQueries(PolicySearchPlan plan) {
        if (!plan.benefitGroups().contains(BenefitGroup.GENERAL_BENEFIT)) {
            return "";
        }
        return generalBenefitSubQueries().stream()
                .map(query -> query.name() + "=" + query.query())
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private String countText(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "";
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private double priority(String query, double base, String... strongTerms) {
        return containsAny((query == null ? "" : query).replaceAll("\\s+", ""), strongTerms) ? 1.25 : base;
    }

    private double rrf(int rank, double weight) {
        return weight / (60.0 + rank) * 20.0;
    }

    private String policyText(Policy policy) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(policy.getTitle())) builder.append(policy.getTitle()).append(' ');
        if (StringUtils.hasText(policy.getSummary())) builder.append(policy.getSummary()).append(' ');
        if (policy.getCondition() != null) {
            if (StringUtils.hasText(policy.getCondition().getConditionSummary())) builder.append(policy.getCondition().getConditionSummary()).append(' ');
            if (StringUtils.hasText(policy.getCondition().getIncomeCondition())) builder.append(policy.getCondition().getIncomeCondition()).append(' ');
        }
        return builder.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private record RegionPoolCounts(int total, int exactSigungu, int parentSido, int nationwide, int multiple) {
        static RegionPoolCounts from(List<RegionEligiblePolicyCandidate> rows) {
            int exactSigungu = 0;
            int parentSido = 0;
            int nationwide = 0;
            int multiple = 0;
            for (RegionEligiblePolicyCandidate row : rows) {
                switch (row.compatibility()) {
                    case EXACT_SIGUNGU, CHILD_SIGUNGU_MATCH -> exactSigungu++;
                    case PARENT_SIDO, EXACT_SIDO -> parentSido++;
                    case NATIONWIDE -> nationwide++;
                    case MULTIPLE_REGION_MATCH, MULTIPLE_SIGUNGU_MATCH, MULTIPLE_CHILD_SIGUNGU_MATCH, MULTIPLE_SIDO_MATCH -> multiple++;
                    default -> {
                    }
                }
            }
            return new RegionPoolCounts(rows.size(), exactSigungu, parentSido, nationwide, multiple);
        }
    }

    private record VectorSearchKey(String query, int topK, boolean applyThreshold) {
    }

    private record EconomicSubQuery(String name, String label, String query, List<String> terms, double weight) {
    }

    private record GeneralBenefitSubQuery(String name, String label, String query, List<String> terms, double weight) {
    }

    private record FallbackCandidate(Integer policyId, double score, List<String> matchedTerms) {
    }

    private record ScoreTerms(double score, List<String> terms) {
        ScoreTerms plus(double delta, String term) {
            List<String> merged = new ArrayList<>(terms);
            merged.add(term);
            return new ScoreTerms(Math.min(1.0, score + delta), merged);
        }
    }
}
