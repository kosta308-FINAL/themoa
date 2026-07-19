package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.common.exception.YouthCenterApiException;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PolicyRagSearchServiceConfigurationTest {
    @Test
    void searchFailsClearlyWhenRagIsDisabled() {
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        SearchDomainIntentPolicy domainIntentPolicy = new SearchDomainIntentPolicy();
        UserEducationStageDetector educationStageDetector = new UserEducationStageDetector();
        RagProperties properties = new RagProperties();
        PolicyDomainClassifier domainClassifier = new PolicyDomainClassifier();
        RegionMatchEvaluator regionMatchEvaluator = mock(RegionMatchEvaluator.class);
        PolicyTargetEligibilityFilter targetEligibilityFilter = new PolicyTargetEligibilityFilter();
        PolicyRagSearchService service = new PolicyRagSearchService(
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
                        new RegionCoverageResultSelector()));

        assertThatThrownBy(() -> service.search(new PolicySearchRequest("청년 지원금", null)))
                .isInstanceOf(YouthCenterApiException.class)
                .hasMessageContaining("RAG 기능이 비활성화되어 있습니다.")
                .hasMessageContaining("RAG_ENABLED=true 설정을 확인하세요.");
    }
}
