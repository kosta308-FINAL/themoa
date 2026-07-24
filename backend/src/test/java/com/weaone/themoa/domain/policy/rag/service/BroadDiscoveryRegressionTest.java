package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.CandidateSource;
import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResponse;
import com.weaone.themoa.domain.policy.rag.dto.RecommendationTier;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import com.weaone.themoa.domain.policy.rag.dto.TargetStageMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatusResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BroadDiscoveryRegressionTest {
    private static final String QUERY = "서울에 사는 27살 직장인이 받을 수 있는 혜택";

    @Test
    void broadGeneralBenefitDoesNotDropAllRegionEligibleCandidatesAtTopicGate() {
        Policy generalLiving = policy(1, "서울 청년 생활지원 혜택", PolicyCategory.복지, "서울 청년 누구나 생활 복지 혜택");
        Policy housing = policy(2, "서울 청년 주거 상담", PolicyCategory.주거, "청년 주거 상담 서비스");
        Policy transit = policy(3, "청년 교통비 지원", PolicyCategory.복지, "대중교통 교통비 환급");
        Policy jobOnly = policy(4, "청년 구직활동 지원", PolicyCategory.일자리, "미취업 구직자 전용 취업 지원");
        Policy startupOnly = policy(5, "청년 창업기업 자금", PolicyCategory.일자리, "창업기업 사업자 대상 자금 지원");
        java.util.ArrayList<EvaluatedPolicyCandidate> candidates = new java.util.ArrayList<>();
        candidates.add(candidate(generalLiving, 0.18, 0.0));
        candidates.add(candidate(housing, 0.16, 0.0));
        candidates.add(candidate(transit, 0.15, 0.0));
        candidates.add(candidate(jobOnly, 0.7, 0.0));
        candidates.add(candidate(startupOnly, 0.72, 0.0));
        for (int i = 6; i <= 28; i++) {
            candidates.add(candidate(policy(i, "특수 대상 정책 " + i, PolicyCategory.복지, "수급 가구 특수 대상"), 0.0, 0.0));
        }

        PolicyRankingResult result = rankingService().rank(context(plan(Set.of(BenefitGroup.GENERAL_BENEFIT))),
                new PolicyEvaluationResult(candidates, new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).isNotEmpty();
        assertThat(result.rankedCandidates().stream().limit(3).map(item -> item.candidate().policy().getId()).toList())
                .containsExactlyInAnyOrder(1, 2, 3);
        assertThat(result.rankedCandidates().stream().limit(3))
                .noneMatch(item -> item.candidate().policy().getTitle().contains("구직")
                        || item.candidate().policy().getTitle().contains("창업"));
        assertThat(result.metrics().topicThresholdFailedCount).isGreaterThan(0);
    }

    @Test
    void semanticWeightIsAppliedOnceAndDiagnosticsValuesAreSeparated() {
        Policy policy = policy(1, "청년 생활 복지", PolicyCategory.복지, "청년 누구나 생활 복지");
        CandidateEvidence evidence = new CandidateEvidence(1, List.of(
                new CandidateSourceEvidence(CandidateSource.VECTOR_ORIGINAL_QUERY, 1, 0.7, 0.7, "query")),
                0.7, 0.7, 0.245, 0.7, 0.0, 0.0);

        PolicyRankingResult result = rankingService().rank(context(plan(Set.of(BenefitGroup.GENERAL_BENEFIT))),
                new PolicyEvaluationResult(List.of(candidate(policy, evidence)), new PolicySearchFilterMetrics()));

        PolicyRankingEvaluation ranking = result.rankedCandidates().get(0).ranking();
        assertThat(ranking.rawSemanticScore()).isEqualTo(0.7);
        assertThat(ranking.normalizedSemanticScore()).isEqualTo(0.7);
        assertThat(ranking.weightedSemanticScore()).isEqualTo(0.245);
        assertThat(ranking.weightedSemanticScore()).isGreaterThan(0.7 * 0.35 * 0.35);
    }

    @Test
    void postRankingFallbackRunsWhenFirstRankingHasNoVisibleCandidate() {
        PolicySearchPlan plan = plan(Set.of(BenefitGroup.GENERAL_BENEFIT));
        PolicySearchIntent intent = new PolicySearchIntent(QUERY, Set.of(), Set.of("혜택"), Set.of("청년", "혜택"),
                "청년 혜택", "혜택");
        PolicySearchPlanService planService = mock(PolicySearchPlanService.class);
        PolicySearchConditionParser.ParsedPolicySearchCondition parsed =
                new PolicySearchConditionParser.ParsedPolicySearchCondition(plan.condition(), PolicyQuerySemantics.empty(), "TEST", false, null);
        when(planService.build(any(), anyInt())).thenReturn(new PolicySearchPlanService.PlannedSearch(parsed, plan, PolicyQuerySemantics.empty()));

        PolicySearchCandidateRetriever retriever = mock(PolicySearchCandidateRetriever.class);
        List<Policy> firstPolicies = java.util.stream.IntStream.rangeClosed(1, 28)
                .mapToObj(id -> policy(id, "무관 후보 " + id, PolicyCategory.복지, "특수 대상"))
                .toList();
        Policy fallbackPolicy = policy(100, "서울 청년 생활지원 혜택", PolicyCategory.복지, "서울 청년 누구나 생활 복지 혜택");
        when(retriever.retrieve(any(), any(), any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(collection(firstPolicies, 0.0, 0.0));
        when(retriever.retrieveBroadFallback(any(), any(), any(), any(), anyInt()))
                .thenReturn(collection(List.of(fallbackPolicy), 0.55, 0.55));

        PolicyEligibilityEvaluator eligibilityEvaluator = mock(PolicyEligibilityEvaluator.class);
        when(eligibilityEvaluator.evaluate(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    PolicyCandidateCollection collection = invocation.getArgument(1);
                    return new PolicyEvaluationResult(collection.policies().stream()
                            .map(policy -> candidate(policy, collection.evidenceByPolicyId().get(policy.getId())))
                            .toList(), new PolicySearchFilterMetrics());
                });

        PolicySearchRuntimeSupport runtime = mock(PolicySearchRuntimeSupport.class);
        when(runtime.buildIntent(any())).thenReturn(intent);
        when(runtime.resolveUserRegion(any())).thenReturn(new ResolvedUserRegion("서울특별시", null, null));
        when(runtime.classifyTargetAudiences(any())).thenReturn(Map.of());
        when(runtime.classifyEmploymentAudiences(any())).thenReturn(Map.of());
        when(runtime.detectEmploymentStatus(any())).thenReturn(new UserEmploymentStatusResult(UserEmploymentStatus.EMPLOYED, true, 1.0, List.of("직장인")));
        when(runtime.selectRegionCoverage(any(), anyInt(), anyInt(), any(), any())).thenAnswer(invocation ->
                new RegionCoverageResultSelector.Selection(invocation.getArgument(0), 0, 0, 0));

        PolicyRagSearchService service = new PolicyRagSearchService(properties(), planService, retriever,
                eligibilityEvaluator, rankingService(), new PolicySearchResultAssembler(new PolicyDomainClassifier()),
                new PolicySearchDiagnosticsFactory(), new PolicySearchExplainService(new PolicyDomainClassifier()), runtime,
                readySearchReadinessService());

        PolicySearchResponse response = service.search(new PolicySearchRequest(QUERY, 10));

        assertThat(response.results()).extracting("policyId").contains(100);
        assertThat(response.diagnostics().rankingFallbackExecuted()).isTrue();
        assertThat(response.diagnostics().fallbackPassedCandidateCount()).isEqualTo(1);
    }

    @Test
    void explicitEmploymentAndStartupIntentStillAllowsMatchingPolicies() {
        Policy job = policy(1, "서울 청년 이직 직무교육", PolicyCategory.일자리, "재직 청년 이직 직무교육 취업 지원");
        Policy startup = policy(2, "청년 창업 사업화 지원", PolicyCategory.일자리, "창업 준비 청년 사업화 지원");

        PolicyRankingResult jobResult = rankingService().rank(context(plan("서울 직장인인데 이직 지원 정책이 궁금해.",
                        Set.of(BenefitGroup.GENERAL_BENEFIT))),
                new PolicyEvaluationResult(List.of(candidate(job, 0.55, 0.4)), new PolicySearchFilterMetrics()));
        PolicyRankingResult startupResult = rankingService().rank(context(plan("직장 다니면서 창업 준비 중인데 받을 수 있는 지원이 있을까?",
                        Set.of(BenefitGroup.GENERAL_BENEFIT))),
                new PolicyEvaluationResult(List.of(candidate(startup, 0.55, 0.4)), new PolicySearchFilterMetrics()));

        assertThat(jobResult.rankedCandidates()).hasSize(1);
        assertThat(startupResult.rankedCandidates()).hasSize(1);
    }

    private PolicyRankingService rankingService() {
        return new PolicyRankingService(properties(), new PolicyDomainClassifier(), new SearchDomainIntentPolicy());
    }

    private SearchReadinessService readySearchReadinessService() {
        SearchReadinessService readinessService = mock(SearchReadinessService.class);
        when(readinessService.readiness()).thenReturn(new SearchReadinessResponse(true, 1, 1, 1, 1, List.of()));
        return readinessService;
    }

    private RagProperties properties() {
        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        return properties;
    }

    private PolicySearchExecutionContext context(PolicySearchPlan plan) {
        return new PolicySearchExecutionContext(new PolicySearchRequest(plan.originalQuery(), 10), plan, 1L);
    }

    private PolicySearchPlan plan(Set<BenefitGroup> benefitGroups) {
        return plan(QUERY, benefitGroups);
    }

    private PolicySearchPlan plan(String query, Set<BenefitGroup> benefitGroups) {
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY, query, "청년 혜택",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), benefitGroups,
                Set.of(), Set.of("청년", "혜택"), Set.of(), condition(), Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchCondition condition() {
        return new PolicySearchCondition("서울특별시", null, null, 27, "EMPLOYED", null, null, "general",
                Set.of(), Set.of("혜택"), Set.of("청년", "혜택"), "서울", "RESOLVED", "SIDO", Set.of(),
                true, true, true, false, false, false, PolicySearchMode.HYBRID, 10);
    }

    private PolicyCandidateCollection collection(List<Policy> policies, double semantic, double lexical) {
        Map<Integer, CandidateEvidence> evidence = new LinkedHashMap<>();
        Map<Integer, Double> semanticScores = new LinkedHashMap<>();
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>();
        Map<Integer, Double> titleScores = new LinkedHashMap<>();
        Map<Integer, Set<CandidateSource>> sources = new LinkedHashMap<>();
        for (Policy policy : policies) {
            CandidateEvidence item = new CandidateEvidence(policy.getId(), List.of(
                    new CandidateSourceEvidence(CandidateSource.BROAD_FALLBACK, 1, semantic, semantic, "TEST")),
                    semantic, semantic, semantic * 0.35, semantic, lexical, 0.0);
            evidence.put(policy.getId(), item);
            semanticScores.put(policy.getId(), semantic);
            lexicalScores.put(policy.getId(), lexical);
            titleScores.put(policy.getId(), 0.0);
            sources.put(policy.getId(), Set.of(CandidateSource.BROAD_FALLBACK));
        }
        return new PolicyCandidateCollection(policies, evidence, semanticScores, lexicalScores, titleScores, sources,
                new CandidateCollectionMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        policies.size(), 0, 0, 28, 0, 28, 0, 0, 28, 28, false, 0, 0),
                false, false, null);
    }

    private EvaluatedPolicyCandidate candidate(Policy policy, double semantic, double lexical) {
        return candidate(policy, new CandidateEvidence(policy.getId(), List.of(), semantic, semantic, semantic * 0.35,
                semantic, lexical, 0.0));
    }

    private EvaluatedPolicyCandidate candidate(Policy policy, CandidateEvidence evidence) {
        PolicyEligibilityEvaluation eligibility = new PolicyEligibilityEvaluation(policy.getId(), true,
                new com.weaone.themoa.domain.policy.policy.region.RegionMatchResult(
                        com.weaone.themoa.domain.policy.policy.region.RegionCompatibility.EXACT_SIDO, true, 100, "서울"),
                ConditionMatchResult.match("나이 일치"),
                ConditionMatchResult.match("재직자 가능"),
                ConditionMatchResult.unknown("학생 미입력"),
                TargetStageMatchResult.unknown("교육 미입력"),
                new EmploymentAudienceMatch(com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus.MATCH, "재직자 가능"),
                RecommendationTier.PRIMARY, List.of(), List.of(), null);
        return new EvaluatedPolicyCandidate(policy, evidence, eligibility);
    }

    private Policy policy(int id, String title, PolicyCategory category, String conditionSummary) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", category, title + " " + conditionSummary, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(19, 39, null, null, null, conditionSummary, false));
        return policy;
    }
}
