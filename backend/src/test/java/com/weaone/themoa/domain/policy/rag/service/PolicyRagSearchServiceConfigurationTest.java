package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.common.exception.YouthCenterApiException;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class PolicyRagSearchServiceConfigurationTest {
    @Test
    void searchFailsWithPolicySearchNotReadyWhenReadinessIsFalse() {
        PolicyRagSearchService service = serviceWithReadiness(
                new SearchReadinessResponse(false, 1, 0, 0, 0, java.util.List.of("SEARCH_PROJECTION_REBUILD")));

        assertThatThrownBy(() -> service.search(new PolicySearchRequest("청년 지원금", null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.POLICY_SEARCH_NOT_READY));
    }

    @Test
    void searchFailsClearlyWhenRagIsDisabled() {
        PolicyRagSearchService service = serviceWithReadiness(
                new SearchReadinessResponse(true, 1, 1, 1, 1, java.util.List.of()));

        assertThatThrownBy(() -> service.search(new PolicySearchRequest("청년 지원금", null)))
                .isInstanceOf(YouthCenterApiException.class)
                .hasMessageContaining("RAG 기능이 비활성화되어 있습니다.")
                .hasMessageContaining("RAG_ENABLED=true 설정을 확인하세요.");
    }

    private PolicyRagSearchService serviceWithReadiness(SearchReadinessResponse readiness) {
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        SearchDomainIntentPolicy domainIntentPolicy = new SearchDomainIntentPolicy();
        UserEducationStageDetector educationStageDetector = new UserEducationStageDetector();
        RagProperties properties = new RagProperties();
        PolicyDomainClassifier domainClassifier = new PolicyDomainClassifier();
        RegionMatchEvaluator regionMatchEvaluator = mock(RegionMatchEvaluator.class);
        PolicyTargetEligibilityFilter targetEligibilityFilter = new PolicyTargetEligibilityFilter();
        SearchReadinessService readinessService = mock(SearchReadinessService.class);
        when(readinessService.readiness()).thenReturn(readiness);
        return new PolicyRagSearchService(
                properties,
                new PolicySearchPlanService(mock(CompositePolicySearchConditionParser.class),
                        new PolicyQueryClassifier(new PolicyKeywordNormalizer()), domainIntentPolicy, educationStageDetector,
                        new SupportIntentDetector(), new BenefitGroupDetector()),
                new PolicySearchCandidateRetriever(mock(PolicyRepository.class), vectorStoreProvider, properties,
                        mock(PolicyLexicalSearchService.class), null, null),
                new PolicyEligibilityEvaluator(properties, regionMatchEvaluator, targetEligibilityFilter),
                new PolicyRankingService(properties, domainClassifier, domainIntentPolicy),
                new PolicySearchResultAssembler(domainClassifier),
                new PolicySearchDiagnosticsFactory(),
                new PolicySearchExplainService(domainClassifier),
                new PolicySearchRuntimeSupport(mock(PolicyRepository.class), regionMatchEvaluator,
                        mock(PolicySearchIntentBuilder.class), mock(PolicyTargetAudienceClassifier.class),
                        mock(PolicyEmploymentAudienceClassifier.class), new UserEmploymentStatusDetector(),
                        new RegionCoverageResultSelector()),
                readinessService);
    }
}
