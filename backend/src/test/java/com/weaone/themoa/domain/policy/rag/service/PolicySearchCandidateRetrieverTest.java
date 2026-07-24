package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.rag.dto.CandidateSource;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicySearchCandidateRetrieverTest {
    @Test
    void broadDiscoveryUsesLexicalWithoutAddingWholeRegionPool() {
        PolicyRepository repository = mock(PolicyRepository.class);
        Policy policy = new Policy("P1");
        org.springframework.test.util.ReflectionTestUtils.setField(policy, "id", 1);
        when(repository.findActivePolicyIds(any())).thenReturn(List.of(1));
        when(repository.findWithRelationsByIdIn(any())).thenReturn(List.of(policy));

        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);

        PolicyLexicalSearchService lexicalSearchService = mock(PolicyLexicalSearchService.class);
        when(lexicalSearchService.search(any(), any(), anyInt())).thenReturn(new PolicyLexicalSearchService.LexicalSearchResult(
                List.of(1),
                Map.of(1, 0.8),
                Map.of(1, 1.0),
                Map.of(1, Set.of(CandidateSource.MYSQL_TITLE)),
                Map.of(CandidateSource.MYSQL_TITLE, 1)));
        RegionEligiblePolicyCandidateService regionService = mock(RegionEligiblePolicyCandidateService.class);
        when(regionService.findSearchEligibleCandidates(any())).thenReturn(java.util.stream.IntStream.rangeClosed(1, 500)
                .mapToObj(id -> new RegionEligiblePolicyCandidate(id, id % 2 == 0 ? RegionCompatibility.NATIONWIDE : RegionCompatibility.EXACT_SIDO))
                .toList());

        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        PolicySearchCandidateRetriever retriever = new PolicySearchCandidateRetriever(
                repository, vectorStoreProvider, properties, lexicalSearchService, regionService, null);

        PolicyCandidateCollection collection = retriever.retrieve(regionalPlan(), intent(), new ResolvedUserRegion("서울특별시", null, null), 0, 10, 10);

        assertThat(collection.policies()).hasSize(1);
        assertThat(collection.metrics().lexicalCandidateCount()).isEqualTo(1);
        assertThat(collection.candidateSources().get(1)).contains(CandidateSource.LEXICAL_INDEX, CandidateSource.EXACT_TITLE);
        assertThat(collection.evidenceByPolicyId().get(1).sourceEvidence())
                .extracting(CandidateSourceEvidence::source)
                .contains(CandidateSource.MYSQL_TITLE, CandidateSource.LEXICAL_INDEX, CandidateSource.EXACT_TITLE);
        assertThat(collection.fallbackReason()).isEqualTo("VECTOR_SEARCH_DISABLED");
    }

    @Test
    void broadDiscoveryWithNoLexicalCandidateDoesNotFallbackToWholeRegionPool() {
        PolicyRepository repository = mock(PolicyRepository.class);
        when(repository.findWithRelationsByIdIn(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Integer> ids = invocation.getArgument(0);
            return ids.stream()
                    .limit(20)
                    .map(id -> {
                        Policy policy = new Policy("P" + id);
                        ReflectionTestUtils.setField(policy, "id", id);
                        String title = id <= 12 ? "청년 주거비 지원 " + id : "청년 무관 정책 " + id;
                        policy.updateBasic(title, "기관", id <= 12 ? PolicyCategory.주거 : PolicyCategory.복지,
                                title, null, null, null, true, true, "OPEN");
                        return policy;
                    })
                    .toList();
        });
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        PolicyLexicalSearchService lexicalSearchService = mock(PolicyLexicalSearchService.class);
        when(lexicalSearchService.search(any(), any(), anyInt())).thenReturn(new PolicyLexicalSearchService.LexicalSearchResult(
                List.of(), Map.of(), Map.of(), Map.of(), Map.of()));
        RegionEligiblePolicyCandidateService regionService = mock(RegionEligiblePolicyCandidateService.class);
        when(regionService.findSearchEligibleCandidates(any())).thenReturn(java.util.stream.IntStream.rangeClosed(1, 500)
                .mapToObj(id -> new RegionEligiblePolicyCandidate(id, RegionCompatibility.NATIONWIDE))
                .toList());

        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        PolicySearchCandidateRetriever retriever = new PolicySearchCandidateRetriever(
                repository, vectorStoreProvider, properties, lexicalSearchService, regionService, null);

        PolicyCandidateCollection collection = retriever.retrieve(regionalPlan(), intent(),
                new ResolvedUserRegion("서울특별시", null, null), 0, 10, 10);

        assertThat(collection.policies()).hasSizeBetween(1, 12);
        assertThat(collection.policies()).hasSizeLessThan(500);
        assertThat(collection.candidateSources().values())
                .anyMatch(sources -> sources.contains(CandidateSource.BROAD_FALLBACK));
        assertThat(collection.candidateSources()).doesNotContainKey(500);
    }

    @Test
    void broadFallbackUsesProjectionRelevanceInsteadOfFrontIdOrderAndCarriesScore() {
        PolicyRepository repository = mock(PolicyRepository.class);
        when(repository.findWithRelationsByIdIn(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Integer> ids = invocation.getArgument(0);
            return ids.stream().map(id -> policy(id, id == 490 ? "청년 교통비 환급" : "청년 일반 정책")).toList();
        });
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        PolicyLexicalSearchService lexicalSearchService = mock(PolicyLexicalSearchService.class);
        when(lexicalSearchService.search(any(), any(), anyInt())).thenReturn(new PolicyLexicalSearchService.LexicalSearchResult(
                List.of(), Map.of(), Map.of(), Map.of(), Map.of()));
        RegionEligiblePolicyCandidateService regionService = mock(RegionEligiblePolicyCandidateService.class);
        when(regionService.findSearchEligibleCandidates(any())).thenReturn(java.util.stream.IntStream.rangeClosed(1, 500)
                .mapToObj(id -> new RegionEligiblePolicyCandidate(id, RegionCompatibility.NATIONWIDE))
                .toList());
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        when(projectionRepository.findAllActive()).thenReturn(List.of(
                projection(10, "청년 일반 정책", "청년 지원 정책"),
                projection(490, "K-패스 교통비 환급", "대중교통 교통비 환급 할인 비용 지원")
        ));

        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        PolicySearchCandidateRetriever retriever = new PolicySearchCandidateRetriever(
                repository, vectorStoreProvider, properties, lexicalSearchService, regionService, projectionRepository);

        PolicyCandidateCollection collection = retriever.retrieve(economicRegionalPlan(), intent(),
                new ResolvedUserRegion("경기도", "수원시", null), 0, 10, 10);

        assertThat(collection.candidateSources().get(490)).contains(CandidateSource.BROAD_FALLBACK);
        assertThat(collection.evidenceByPolicyId().get(490).semanticScore()).isGreaterThan(0);
        assertThat(collection.evidenceByPolicyId().get(490).lexicalScore()).isGreaterThan(0);
    }

    @Test
    void regionExplicitSearchKeepsRelevantLocalRegionCandidateBeforeFallbackThreshold() {
        PolicyRepository repository = mock(PolicyRepository.class);
        when(repository.findWithRelationsByIdIn(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Integer> ids = invocation.getArgument(0);
            return ids.stream()
                    .map(id -> policy(id, id == 100 ? "수원 청년 금융 지원" : "전국 청년 금융 지원 " + id))
                    .toList();
        });
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        PolicyLexicalSearchService lexicalSearchService = mock(PolicyLexicalSearchService.class);
        List<Integer> nationalIds = java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList();
        Map<Integer, Double> lexicalScores = nationalIds.stream()
                .collect(java.util.stream.Collectors.toMap(id -> id, ignored -> 0.8));
        when(lexicalSearchService.search(any(), any(), anyInt())).thenReturn(new PolicyLexicalSearchService.LexicalSearchResult(
                nationalIds, lexicalScores, Map.of(), Map.of(), Map.of()));
        RegionEligiblePolicyCandidateService regionService = mock(RegionEligiblePolicyCandidateService.class);
        when(regionService.findSearchEligibleCandidates(any())).thenReturn(java.util.stream.Stream.concat(
                        nationalIds.stream().map(id -> new RegionEligiblePolicyCandidate(id, RegionCompatibility.NATIONWIDE)),
                        java.util.stream.Stream.of(new RegionEligiblePolicyCandidate(100, RegionCompatibility.EXACT_SIGUNGU)))
                .toList());
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        when(projectionRepository.findAllActive()).thenReturn(List.of(
                projection(policy(100, "수원 청년 금융 지원"), "수원 청년 금융 지원", "청년 금융 지원")));

        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        PolicySearchCandidateRetriever retriever = new PolicySearchCandidateRetriever(
                repository, vectorStoreProvider, properties, lexicalSearchService, regionService, projectionRepository);

        PolicyCandidateCollection collection = retriever.retrieve(economicRegionalPlan(), intent(),
                new ResolvedUserRegion("경기도", "수원시", null), 0, 10, 10);

        assertThat(collection.policies()).extracting(Policy::getId).contains(100);
        assertThat(collection.candidateSources().get(100)).contains(CandidateSource.BROAD_FALLBACK);
    }

    private PolicySearchPlan plan() {
        PolicySearchCondition condition = new PolicySearchCondition(null, null, null, null, null, null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), null, null, null, Set.of(),
                false, false, false, false, false, false, PolicySearchMode.HYBRID, 10);
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY, "청년 정책", "청년 정책",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), Set.of(),
                Set.of("청년"), Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchPlan regionalPlan() {
        PolicySearchCondition condition = new PolicySearchCondition("서울특별시", null, null, null, null, null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), "서울", "RESOLVED", "SIDO", Set.of(),
                true, false, false, false, false, false, PolicySearchMode.HYBRID, 10);
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY, "서울 청년 혜택", "청년 혜택",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL),
                Set.of(BenefitGroup.GENERAL_BENEFIT), Set.of(),
                Set.of("청년"), Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchPlan economicRegionalPlan() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 27, "UNEMPLOYED", null, null, "general",
                Set.of("CASH"), Set.of("지원금"), Set.of("지원금"), "수원", "RESOLVED", "SIGUNGU", Set.of(),
                true, true, true, false, false, true, PolicySearchMode.HYBRID, 10);
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY, "수원 27살 무직 청년 지원금", "청년 지원금",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.CASH_ASSISTANCE),
                Set.of(BenefitGroup.ECONOMIC_SUPPORT), Set.of(),
                Set.of("지원금"), Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchIntent intent() {
        return new PolicySearchIntent("청년 정책", Set.of(), Set.of("청년"), Set.of("청년"),
                "청년 정책", "청년");
    }

    private Policy policy(Integer id, String title) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", PolicyCategory.복지, title, null, null, null, true, true, "OPEN");
        return policy;
    }

    private PolicySearchProjection projection(Integer id, String title, String support) {
        Policy policy = policy(id, title);
        return projection(policy, title, support);
    }

    private PolicySearchProjection projection(Policy policy, String title, String support) {
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(title, title, "", "복지", "", support, "청년", "", "", "기관",
                String.join(" ", title, support), "test", false);
        return projection;
    }
}
