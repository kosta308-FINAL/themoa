package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.exception.YouthCenterApiException;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchDiagnostics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResponse;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResultItem;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatusResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 자연어 정책 검색 흐름을 조율하는 오케스트레이터다.
 *
 * <p>호출 순서는 PolicySearchPlanService -> PolicySearchCandidateRetriever ->
 * PolicyEligibilityEvaluator -> PolicyRankingService -> PolicySearchResultAssembler ->
 * PolicySearchDiagnosticsFactory -> PolicySearchResponse다.</p>
 *
 * <p>입력은 PolicySearchRequest이고 출력은 PolicySearchResponse다. 이 클래스는 plan 생성과 후보 조회를
 * 연결하지만 자격 판정, 점수 계산, DTO 조립, Explain Map 조립을 직접 수행하지 않는다. DB 조회는
 * 후보 수집기와 explain 대상 정책 조회에 한정하고, 외부 API 호출은 planService 내부 정책을 따른다.</p>
 *
 * <p>수정 시 새 검색 규칙을 여기에 넣지 말고 해당 단계 서비스에 추가해야 한다. 이 클래스에 private
 * scoring/filtering 메서드가 다시 생기면 검색 흐름을 읽기 어려워지고 테스트 경계가 흐려진다.</p>
 */
@Service
public class PolicyRagSearchService {
    private final RagProperties properties;
    private final PolicySearchPlanService planService;
    private final PolicySearchCandidateRetriever candidateRetriever;
    private final PolicyEligibilityEvaluator eligibilityEvaluator;
    private final PolicyRankingService rankingService;
    private final PolicySearchResultAssembler resultAssembler;
    private final PolicySearchDiagnosticsFactory diagnosticsFactory;
    private final PolicySearchExplainService explainService;
    private final PolicySearchRuntimeSupport runtimeSupport;
    private final SearchReadinessService readinessService;

    public PolicyRagSearchService(RagProperties properties,
                                  PolicySearchPlanService planService,
                                  PolicySearchCandidateRetriever candidateRetriever,
                                  PolicyEligibilityEvaluator eligibilityEvaluator,
                                  PolicyRankingService rankingService,
                                  PolicySearchResultAssembler resultAssembler,
                                  PolicySearchDiagnosticsFactory diagnosticsFactory,
                                  PolicySearchExplainService explainService,
                                  PolicySearchRuntimeSupport runtimeSupport,
                                  SearchReadinessService readinessService) {
        this.properties = properties;
        this.planService = planService;
        this.candidateRetriever = candidateRetriever;
        this.eligibilityEvaluator = eligibilityEvaluator;
        this.rankingService = rankingService;
        this.resultAssembler = resultAssembler;
        this.diagnosticsFactory = diagnosticsFactory;
        this.explainService = explainService;
        this.runtimeSupport = runtimeSupport;
        this.readinessService = readinessService;
    }

    public PolicySearchResponse search(PolicySearchRequest request) {
        SearchReadinessResponse readiness = readinessService.readiness();
        if (!readiness.ready()) {
            throw new BusinessException(ErrorCode.POLICY_SEARCH_NOT_READY);
        }
        return execute(request).response();
    }

    public Map<String, Object> explain(String query, Integer policyId, String sourcePolicyId) {
        Policy target = runtimeSupport.findPolicy(policyId, sourcePolicyId);
        SearchArtifacts artifacts = execute(new PolicySearchRequest(query, 100, 0, 100));
        RankedPolicyCandidate ranked = artifacts.rankedByPolicyId().get(target.getId());
        PolicyEligibilityEvaluation evaluated = artifacts.evaluatedByPolicyId().get(target.getId());
        CandidateEvidence evidence = artifacts.candidates().evidenceByPolicyId()
                .getOrDefault(target.getId(), new CandidateEvidence(target.getId(), List.of(), 0.0, 0.0, 0.0));
        PolicyEligibilityEvaluation eligibility = evaluated == null
                ? filteredEvaluation(target, artifacts.context(), artifacts.userRegion())
                : evaluated;
        return explainService.explain(artifacts.context().plan(), target, evidence, eligibility,
                ranked == null ? null : ranked.ranking(), artifacts.response().diagnostics(), ranked != null);
    }

    private SearchArtifacts execute(PolicySearchRequest request) {
        if (!properties.isEnabled()) {
            throw new YouthCenterApiException("""
                    RAG 기능이 비활성화되어 있습니다.
                    RAG_ENABLED=true 설정을 확인하세요.""");
        }
        Instant start = Instant.now();
        int page = request.resolvedPage();
        int size = request.resolvedSize(properties.getSearch().getResultSize());
        int resultSize = Math.max(size, request.resultSize() == null ? size : request.resultSize());
        PolicySearchPlanService.PlannedSearch planned = planService.build(request.query(), resultSize);
        PolicySearchExecutionContext context = new PolicySearchExecutionContext(request, planned.plan(), start.toEpochMilli());
        PolicySearchIntent intent = runtimeSupport.buildIntent(context.plan());
        PolicySearchCondition condition = context.plan().condition();
        ResolvedUserRegion userRegion = runtimeSupport.resolveUserRegion(condition);
        PolicyCandidateCollection candidates = candidateRetriever.retrieve(context.plan(), intent, userRegion, page, size, resultSize);
        List<Integer> policyIds = candidates.policies().stream().map(Policy::getId).toList();
        Map<Integer, PolicyTargetAudienceClassification> targetAudiences = runtimeSupport.classifyTargetAudiences(policyIds);
        Map<Integer, PolicyEmploymentAudience> employmentAudiences = runtimeSupport.classifyEmploymentAudiences(policyIds);
        UserEmploymentStatusResult employmentStatus = runtimeSupport.detectEmploymentStatus(request.query());
        PolicyEvaluationResult evaluated = eligibilityEvaluator.evaluate(context, candidates, userRegion,
                targetAudiences, employmentAudiences, employmentStatus);
        PolicyRankingResult ranked = rankingService.rank(context, evaluated);
        ranked.metrics().firstRankingResultCount = ranked.rankedCandidates().size();
        if (shouldRunPostRankingFallback(context.plan().queryType(), ranked.rankedCandidates().size(), resultSize)) {
            Set<Integer> alreadySelected = candidates.policies().stream()
                    .map(Policy::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            PolicyCandidateCollection fallbackCandidates = candidateRetriever.retrieveBroadFallback(
                    context.plan(), intent, userRegion, alreadySelected, fallbackTarget(resultSize));
            ranked.metrics().rankingFallbackExecuted = true;
            ranked.metrics().fallbackReviewedCandidateCount = fallbackCandidates.policies().size();
            if (!fallbackCandidates.policies().isEmpty()) {
                List<Integer> fallbackPolicyIds = fallbackCandidates.policies().stream().map(Policy::getId).toList();
                Map<Integer, PolicyTargetAudienceClassification> fallbackTargetAudiences = runtimeSupport.classifyTargetAudiences(fallbackPolicyIds);
                Map<Integer, PolicyEmploymentAudience> fallbackEmploymentAudiences = runtimeSupport.classifyEmploymentAudiences(fallbackPolicyIds);
                PolicyEvaluationResult fallbackEvaluated = eligibilityEvaluator.evaluate(context, fallbackCandidates, userRegion,
                        fallbackTargetAudiences, fallbackEmploymentAudiences, employmentStatus);
                PolicyRankingResult fallbackRanked = rankingService.rank(context, fallbackEvaluated);
                ranked.metrics().fallbackPassedCandidateCount = fallbackRanked.rankedCandidates().size();
                targetAudiences = mergeMap(targetAudiences, fallbackTargetAudiences);
                employmentAudiences = mergeMap(employmentAudiences, fallbackEmploymentAudiences);
                evaluated = mergeEvaluations(evaluated, fallbackEvaluated);
                ranked = new PolicyRankingResult(mergeRanked(ranked.rankedCandidates(), fallbackRanked.rankedCandidates()), ranked.metrics());
                candidates = mergeCandidates(candidates, fallbackCandidates, ranked.metrics());
            }
        }
        ranked.metrics().finalCandidateCount = ranked.rankedCandidates().size();
        Map<Integer, PolicyTargetAudienceClassification> finalTargetAudiences = targetAudiences;
        Map<Integer, PolicyEmploymentAudience> finalEmploymentAudiences = employmentAudiences;
        List<PolicySearchResultItem> allResults = ranked.rankedCandidates().stream()
                .map(item -> resultAssembler.assemble(item, finalTargetAudiences, finalEmploymentAudiences))
                .toList();
        RegionCoverageResultSelector.Selection selection = runtimeSupport.selectRegionCoverage(allResults, page, size, context.plan().queryType());
        List<PolicySearchResultItem> orderedResults = selection.orderedResults();
        int from = Math.min(orderedResults.size(), page * size);
        int to = Math.min(orderedResults.size(), from + size);
        List<PolicySearchResultItem> results = orderedResults.subList(from, to);
        String fallbackReason = results.isEmpty()
                ? determineFallbackReason(candidates.fallbackReason(), candidates.semanticScores().size(),
                candidates.metrics().lexicalCandidateCount(), ranked.metrics(), condition)
                : null;
        PolicySearchDiagnostics diagnostics = diagnosticsFactory.create(context.plan(), intent, planned.parsed(),
                candidates.metrics(), ranked.metrics(), selection, results, candidates.semanticScores().size(),
                candidates.retried(), candidates.mysqlFallbackUsed(), fallbackReason,
                Duration.between(start, Instant.now()).toMillis(),
                employmentStatus.status().name(), employmentStatus.explicit(), String.join(", ", employmentStatus.evidence()),
                employmentStatus.explicit() ? "RULE_EXPLICIT" : "UNKNOWN");
        PolicySearchResponse response = new PolicySearchResponse(answer(results, fallbackReason), condition,
                planned.parsed().parserMode(), planned.parsed().fallback(), condition.searchMode().name(),
                context.plan().queryType().name(), candidates.metrics().vectorCandidateCount(), results.size(),
                orderedResults.size(), page, size, to < orderedResults.size(), results, diagnostics);
        return new SearchArtifacts(context, userRegion, candidates, evaluated, ranked, response);
    }

    private boolean shouldRunPostRankingFallback(SearchQueryType queryType, int rankedCount, int resultSize) {
        return (queryType == SearchQueryType.BROAD_DISCOVERY || queryType == SearchQueryType.ELIGIBILITY_SEARCH)
                && rankedCount < 5
                && properties.getSearch().isMysqlFallbackEnabled()
                && resultSize > 0;
    }

    private int fallbackTarget(int resultSize) {
        return Math.max(5, Math.min(12, Math.max(1, resultSize)));
    }

    private <T> Map<Integer, T> mergeMap(Map<Integer, T> left, Map<Integer, T> right) {
        Map<Integer, T> merged = new LinkedHashMap<>(left);
        merged.putAll(right);
        return merged;
    }

    private PolicyEvaluationResult mergeEvaluations(PolicyEvaluationResult left, PolicyEvaluationResult right) {
        List<EvaluatedPolicyCandidate> passed = new ArrayList<>(left.passedCandidates());
        passed.addAll(right.passedCandidates());
        Map<Integer, PolicyEligibilityEvaluation> evaluations = new LinkedHashMap<>(left.evaluationsByPolicyId());
        evaluations.putAll(right.evaluationsByPolicyId());
        return new PolicyEvaluationResult(passed, evaluations, left.metrics());
    }

    private List<RankedPolicyCandidate> mergeRanked(List<RankedPolicyCandidate> left, List<RankedPolicyCandidate> right) {
        Map<Integer, RankedPolicyCandidate> byId = new LinkedHashMap<>();
        for (RankedPolicyCandidate item : left) {
            byId.put(item.candidate().policy().getId(), item);
        }
        for (RankedPolicyCandidate item : right) {
            byId.merge(item.candidate().policy().getId(), item,
                    (existing, added) -> added.ranking().rawFinalScore() > existing.ranking().rawFinalScore() ? added : existing);
        }
        List<RankedPolicyCandidate> ordered = byId.values().stream()
                .sorted((a, b) -> {
                    int tier = Integer.compare(tierPriority(a), tierPriority(b));
                    if (tier != 0) return tier;
                    int title = Double.compare(b.ranking().titleScore(), a.ranking().titleScore());
                    if (title != 0) return title;
                    int score = Double.compare(b.ranking().rawFinalScore(), a.ranking().rawFinalScore());
                    if (score != 0) return score;
                    return Integer.compare(a.candidate().policy().getId(), b.candidate().policy().getId());
                })
                .toList();
        List<RankedPolicyCandidate> ranked = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            RankedPolicyCandidate item = ordered.get(i);
            PolicyRankingEvaluation r = item.ranking();
            ranked.add(new RankedPolicyCandidate(item.candidate(), new PolicyRankingEvaluation(
                    r.topicScore(), r.rawSemanticScore(), r.normalizedSemanticScore(), r.weightedSemanticScore(),
                    r.semanticScore(), r.lexicalScore(), r.titleScore(), r.domainScore(), r.supportIntentScore(),
                    r.eligibilityBreadthScore(), r.eligibilityBreadth(), r.eligibilityBreadthEvidence(),
                    r.regionScore(), r.rawFinalScore(), r.finalScore(), r.recommendationTier(), i + 1,
                    r.rankingReasons())));
        }
        return ranked;
    }

    private int tierPriority(RankedPolicyCandidate item) {
        return item.ranking().recommendationTier() == com.weaone.themoa.domain.policy.rag.dto.RecommendationTier.PRIMARY ? 0 : 1;
    }

    private PolicyCandidateCollection mergeCandidates(PolicyCandidateCollection left,
                                                      PolicyCandidateCollection right,
                                                      PolicySearchFilterMetrics rankingMetrics) {
        List<Policy> policies = new ArrayList<>(left.policies());
        Set<Integer> ids = policies.stream().map(Policy::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        for (Policy policy : right.policies()) {
            if (ids.add(policy.getId())) {
                policies.add(policy);
            }
        }
        Map<Integer, CandidateEvidence> evidence = new LinkedHashMap<>(left.evidenceByPolicyId());
        evidence.putAll(right.evidenceByPolicyId());
        Map<Integer, Double> semantic = new LinkedHashMap<>(left.semanticScores());
        semantic.putAll(right.semanticScores());
        Map<Integer, Double> lexical = new LinkedHashMap<>(left.lexicalScores());
        lexical.putAll(right.lexicalScores());
        Map<Integer, Double> title = new LinkedHashMap<>(left.titleExactScores());
        title.putAll(right.titleExactScores());
        Map<Integer, Set<com.weaone.themoa.domain.policy.rag.dto.CandidateSource>> sources = new LinkedHashMap<>(left.candidateSources());
        sources.putAll(right.candidateSources());
        CandidateCollectionMetrics metrics = new CandidateCollectionMetrics(
                left.metrics().vectorCandidateCount(),
                left.metrics().vectorOriginalCandidateCount(),
                left.metrics().vectorIntentCandidateCount(),
                left.metrics().vectorExpandedCandidateCount(),
                left.metrics().vectorCategoryCandidateCount(),
                left.metrics().vectorNormalizedCandidateCount(),
                left.metrics().lexicalCandidateCount(),
                left.metrics().mysqlTitleCandidateCount(),
                left.metrics().mysqlKeywordCandidateCount(),
                left.metrics().mysqlSummaryCandidateCount(),
                left.metrics().mysqlCategoryCandidateCount(),
                policies.size(),
                left.metrics().duplicateCandidateCount(),
                left.metrics().fallbackAddedCount() + right.metrics().fallbackAddedCount(),
                left.metrics().regionPoolTotal(),
                left.metrics().regionPoolExactSigungu(),
                left.metrics().regionPoolParentSido(),
                left.metrics().regionPoolNationwide(),
                left.metrics().regionPoolMultiple(),
                left.metrics().initialMergedCandidateCount(),
                left.metrics().regionIntersectionCandidateCount(),
                rankingMetrics.rankingFallbackExecuted,
                rankingMetrics.fallbackReviewedCandidateCount,
                rankingMetrics.fallbackPassedCandidateCount);
        return new PolicyCandidateCollection(policies, evidence, semantic, lexical, title, sources, metrics,
                left.retried(), left.mysqlFallbackUsed(), left.fallbackReason());
    }

    private PolicyEligibilityEvaluation filteredEvaluation(Policy policy,
                                                           PolicySearchExecutionContext context,
                                                           ResolvedUserRegion userRegion) {
        PolicyCandidateCollection single = new PolicyCandidateCollection(List.of(policy),
                Map.of(policy.getId(), new CandidateEvidence(policy.getId(), List.of(), 0.0, 0.0, 0.0)),
                Map.of(), Map.of(), Map.of(), Map.of(), new CandidateCollectionMetrics(0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), false, false, null);
        return eligibilityEvaluator.evaluate(context, single, userRegion, Map.of(), Map.of(),
                        runtimeSupport.detectEmploymentStatus(context.request().query()))
                .passedCandidates().stream()
                .map(EvaluatedPolicyCandidate::eligibility)
                .findFirst()
                .orElseGet(() -> new PolicyEligibilityEvaluation(policy.getId(), false,
                        runtimeSupport.evaluateRegion(policy, userRegion),
                        com.weaone.themoa.domain.policy.rag.dto.ConditionMatchResult.unknown("검색 결과에 포함되지 않았습니다."),
                        com.weaone.themoa.domain.policy.rag.dto.ConditionMatchResult.unknown("검색 결과에 포함되지 않았습니다."),
                        com.weaone.themoa.domain.policy.rag.dto.ConditionMatchResult.unknown("검색 결과에 포함되지 않았습니다."),
                        com.weaone.themoa.domain.policy.rag.dto.TargetStageMatchResult.unknown("검색 결과에 포함되지 않았습니다."),
                        new EmploymentAudienceMatch(com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus.UNKNOWN, "검색 결과에 포함되지 않았습니다."),
                        com.weaone.themoa.domain.policy.rag.dto.RecommendationTier.NEEDS_CONFIRMATION,
                        List.of(), List.of("검색 결과에 포함되지 않았습니다."), "NOT_IN_TRACE_OR_BELOW_RESULT_LIMIT"));
    }

    private String determineFallbackReason(String current, int vectorCount, int lexicalCount, PolicySearchFilterMetrics metrics,
                                           PolicySearchCondition condition) {
        if (vectorCount == 0 && lexicalCount == 0) return "NO_CANDIDATES";
        if (lexicalCount == 0 && condition.searchMode() == com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode.KEYWORD) return "NO_LEXICAL_MATCH";
        if (condition.regionExplicit() && metrics.regionEligibleCount == 0 && metrics.regionFiltered > 0) return "ALL_REMOVED_BY_REGION";
        if (metrics.hasNoTopicRelevantCandidate()) return "NO_TOPIC_RELEVANT_CANDIDATES";
        if (condition.regionExplicit() && metrics.regionFiltered > 0 && metrics.regionFiltered >= metrics.ageFiltered
                && metrics.regionFiltered >= metrics.employmentFiltered) return "ALL_REMOVED_BY_REGION";
        if (condition.ageExplicit() && metrics.ageFiltered > 0) return "ALL_REMOVED_BY_AGE";
        if (condition.employmentExplicit() && metrics.employmentFiltered > 0) return "ALL_REMOVED_BY_EMPLOYMENT";
        if (metrics.applicationFiltered > 0) return "ALL_REMOVED_BY_APPLICATION_STATUS";
        return current == null ? "INSUFFICIENT_FINAL_RESULTS" : current;
    }

    private String answer(List<PolicySearchResultItem> results, String fallbackReason) {
        if (!results.isEmpty()) {
            return "검색된 실제 온통청년 정책 중 관련도가 높은 정책을 우선 표시합니다. 최종 신청 자격은 정책 상세와 공식 기관에서 확인해야 합니다.";
        }
        return switch (fallbackReason == null ? "" : fallbackReason) {
            case "ALL_REMOVED_BY_REGION" -> "검색 후보는 있었지만 입력한 지역과 호환되는 정책이 최종 결과에 남지 않았습니다. 지역 데이터 또는 정책 지역 판정 상태를 확인하세요.";
            case "NO_TOPIC_RELEVANT_CANDIDATES" -> "지역 또는 조건 후보는 있었지만 검색 의도와 충분히 관련 있는 정책이 남지 않았습니다. 키워드를 조금 더 구체화해 보세요.";
            case "ALL_REMOVED_BY_AGE" -> "검색 후보는 있었지만 입력한 나이 조건과 명확히 맞지 않아 제외되었습니다.";
            case "ALL_REMOVED_BY_EMPLOYMENT" -> "검색 후보는 있었지만 입력한 취업 상태 조건과 명확히 맞지 않아 제외되었습니다.";
            case "ALL_REMOVED_BY_APPLICATION_STATUS" -> "검색 후보는 있었지만 신청 마감 상태로 제외되었습니다.";
            case "NO_CANDIDATES" -> "검색 후보를 찾지 못했습니다. 키워드를 넓혀 다시 검색해 보세요.";
            default -> "검색 후보는 있었지만 최종 표시 기준을 통과한 정책이 없습니다. 검색 진단의 제외 사유를 확인하세요.";
        };
    }

    private record SearchArtifacts(
            PolicySearchExecutionContext context,
            ResolvedUserRegion userRegion,
            PolicyCandidateCollection candidates,
                PolicyEvaluationResult evaluated,
            PolicyRankingResult ranked,
            PolicySearchResponse response
    ) {
        Map<Integer, PolicyEligibilityEvaluation> evaluatedByPolicyId() {
            return evaluated.evaluationsByPolicyId();
        }

        Map<Integer, RankedPolicyCandidate> rankedByPolicyId() {
            return ranked.rankedCandidates().stream()
                    .collect(Collectors.toMap(item -> item.candidate().policy().getId(), Function.identity()));
        }
    }
}
