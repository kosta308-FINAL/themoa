package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.domain.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchResult;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatusResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralBenefitSearchTest {
    @Test
    void commuteGeneralBenefitSearchRetrievesTransportPolicyAndFiltersOrganizationOnlyPolicy() {
        Policy kpass = policy(1, "K-패스(K패스)", PolicyCategory.복지,
                "전국 대중교통 이용 청년", "대중교통 이용 시 교통비 환급");
        Policy housing = policy(2, "청년 월세 지원", PolicyCategory.주거,
                "19~39세 청년", "월세 주거비 지원");
        Policy culture = policy(3, "청년문화예술패스", PolicyCategory.문화,
                "19세 청년", "문화 예술 관람비 지원");
        Policy savings = policy(4, "청년 자산형성 통장", PolicyCategory.금융,
                "19~39세 청년", "저축 계좌 정부기여금 지원");
        Policy organization = policy(5, "벤처기업 공동채용 지원사업", PolicyCategory.일자리,
                "참여기업", "채용수요가 있는 벤처기업들이 합동으로 취업포털 사이트에 취업 공고를 게시하고 "
                        + "채용박람회를 통해 벤처기업의 우수 인재채용을 지원");

        PolicyRepository repository = mock(PolicyRepository.class);
        when(repository.findWithRelationsByIdIn(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Integer> ids = invocation.getArgument(0);
            return List.of(kpass, housing, culture, savings, organization).stream()
                    .filter(policy -> ids.contains(policy.getId()))
                    .toList();
        });
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        when(projectionRepository.findAllActive()).thenReturn(List.of(
                projection(kpass), projection(housing), projection(culture), projection(savings), projection(organization)));
        RegionEligiblePolicyCandidateService regionService = mock(RegionEligiblePolicyCandidateService.class);
        when(regionService.findEligibleCandidates(any())).thenReturn(List.of(
                new RegionEligiblePolicyCandidate(1, RegionCompatibility.NATIONWIDE),
                new RegionEligiblePolicyCandidate(2, RegionCompatibility.PARENT_SIDO),
                new RegionEligiblePolicyCandidate(3, RegionCompatibility.NATIONWIDE),
                new RegionEligiblePolicyCandidate(4, RegionCompatibility.NATIONWIDE),
                new RegionEligiblePolicyCandidate(5, RegionCompatibility.NATIONWIDE)));

        PolicyLexicalSearchService lexical = new PolicyLexicalSearchService(
                new PolicyLexicalIndexBuilder(projectionRepository, new PolicyKeywordNormalizer()));
        PolicySearchCandidateRetriever retriever = new PolicySearchCandidateRetriever(
                repository, vectorStoreProvider, new RagProperties(), lexical, regionService, projectionRepository);
        PolicySearchPlan plan = plan();
        PolicyCandidateCollection candidates = retriever.retrieve(plan, intent(), new ResolvedUserRegion("경기도", "수원시", null), 0, 10, 10);

        assertThat(candidates.policies()).extracting(Policy::getId).contains(1);
        assertThat(candidates.metrics().generalBenefitLexicalCounts()).contains("LIVING_TRANSPORT");

        RegionMatchEvaluator regionEvaluator = mock(RegionMatchEvaluator.class);
        when(regionEvaluator.evaluate(any(), any())).thenReturn(new RegionMatchResult(RegionCompatibility.NATIONWIDE, true, 100, "전국"));
        PolicyEligibilityEvaluator eligibilityEvaluator = new PolicyEligibilityEvaluator(
                new RagProperties(), regionEvaluator, new PolicyTargetEligibilityFilter(),
                new PolicyApplicantAudienceClassifier(), new UserApplicantTypeDetector());
        PolicyEvaluationResult evaluated = eligibilityEvaluator.evaluate(
                new PolicySearchExecutionContext(new PolicySearchRequest(plan.originalQuery(), 10), plan, 1L),
                candidates,
                new ResolvedUserRegion("경기도", "수원시", null),
                Map.of(),
                Map.of(1, PolicyEmploymentAudience.unknown(), 2, PolicyEmploymentAudience.unknown(),
                        3, PolicyEmploymentAudience.unknown(), 4, PolicyEmploymentAudience.unknown(), 5, PolicyEmploymentAudience.unknown()),
                new UserEmploymentStatusResult(UserEmploymentStatus.EMPLOYED, true, 1.0, List.of("직장인")));

        assertThat(evaluated.passedCandidates()).extracting(EvaluatedPolicyCandidate::policy)
                .extracting(Policy::getId)
                .doesNotContain(5);

        PolicyRankingResult ranked = new PolicyRankingService(new RagProperties(), new PolicyDomainClassifier(), new SearchDomainIntentPolicy())
                .rank(new PolicySearchExecutionContext(new PolicySearchRequest(plan.originalQuery(), 10), plan, 1L), evaluated);

        assertThat(ranked.rankedCandidates().stream().limit(5).map(item -> item.candidate().policy().getId()).toList())
                .contains(1);
    }

    private PolicySearchPlan plan() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 29, "EMPLOYED",
                null, null, "general", Set.of(), Set.of("청년", "혜택"), Set.of("청년", "혜택"),
                "수원", "EXACT", "SIGUNGU", Set.of(), true, true, true, false,
                false, false, PolicySearchMode.HYBRID, 10, null, null, null, null,
                "서울특별시", null, null, "서울특별시", "EXACT");
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY,
                "수원에 살고 서울로 출근하는 29살 직장인이야. 받을 수 있는 혜택 알려줘.",
                "청년 생활 혜택",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL),
                Set.of(BenefitGroup.GENERAL_BENEFIT), Set.of(), Set.of("청년", "혜택"),
                Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchIntent intent() {
        return new PolicySearchIntent("수원에 살고 서울로 출근하는 29살 직장인이야. 받을 수 있는 혜택 알려줘.",
                Set.of("수원", "29살"), Set.of("청년 혜택"),
                Set.of("청년", "혜택", "교통 지원", "생활 지원"), "청년 생활 혜택", "청년 혜택 교통 지원 생활 지원");
    }

    private Policy policy(int id, String title, PolicyCategory category, String target, String summary) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", category, summary, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, null, null, null, target, false));
        return policy;
    }

    private PolicySearchProjection projection(Policy policy) {
        String title = policy.getTitle();
        String target = policy.getCondition() == null ? "" : policy.getCondition().getConditionSummary();
        String support = policy.getSummary();
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(new PolicyKeywordNormalizer().normalize(title), title, "", policy.getCategory().name(),
                "", support, target, "", "", "기관", title + " " + target + " " + support, "test", false);
        return projection;
    }
}
