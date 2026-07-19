package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyNameSearchTest {
    @Test
    void exactTitleAliasesReturnOnlyExactPolicyAndSuppressBroadSearch() {
        PolicyRepository repository = mock(PolicyRepository.class);
        Policy exact = policy(1, "K-패스(K패스)");
        Policy unrelated = policy(2, "청년도약계좌");
        when(repository.findWithRelationsByIdIn(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Integer> ids = invocation.getArgument(0);
            return List.of(exact, unrelated).stream()
                    .filter(policy -> ids.contains(policy.getId()))
                    .toList();
        });

        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        when(projectionRepository.findAllActive()).thenReturn(List.of(
                projection(exact, "K-패스(K패스)", "전국 대중교통 이용 시 교통비 환급"),
                projection(unrelated, "청년도약계좌", "청년 저축 계좌 지원")
        ));
        PolicyLexicalSearchService lexicalSearchService = new PolicyLexicalSearchService(
                new PolicyLexicalIndexBuilder(projectionRepository, new PolicyKeywordNormalizer()));

        PolicySearchCandidateRetriever retriever = new PolicySearchCandidateRetriever(
                repository, vectorStoreProvider, new RagProperties(), lexicalSearchService,
                mock(RegionEligiblePolicyCandidateService.class), projectionRepository);

        for (String query : List.of("K-패스", "K패스", "k 패스", "k-패스")) {
            var collection = retriever.retrieve(plan(query), intent(query), null, 0, 10, 10);

            assertThat(collection.policies()).extracting(Policy::getTitle).containsExactly("K-패스(K패스)");
            assertThat(collection.metrics().titleSearchMode()).isEqualTo("EXACT_TITLE");
            assertThat(collection.metrics().broadSearchSuppressed()).isTrue();
            assertThat(collection.metrics().normalizedQueryTitle()).isEqualTo("k패스");
            assertThat(collection.metrics().exactMatchCount()).isEqualTo(1);
        }
    }

    private PolicySearchPlan plan(String query) {
        PolicySearchCondition condition = new PolicySearchCondition(null, null, null, null, null, null, null, null,
                Set.of(), Set.of(query), Set.of(query), null, null, null, Set.of(),
                false, false, false, false, false, false, PolicySearchMode.KEYWORD, 10);
        return new PolicySearchPlan(SearchQueryType.POLICY_NAME, query, query,
                Set.<SearchDomain>of(), Set.<SearchDomain>of(), Set.<SupportIntent>of(), Set.<BenefitGroup>of(),
                Set.<SupportIntent>of(), Set.of(query), Set.of(), condition,
                Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchIntent intent(String query) {
        return new PolicySearchIntent(query, Set.of(query), Set.of(query), Set.of(query), query, query);
    }

    private Policy policy(Integer id, String title) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", PolicyCategory.복지, title, null, null, null, true, true, "OPEN");
        return policy;
    }

    private PolicySearchProjection projection(Policy policy, String title, String support) {
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(new PolicyKeywordNormalizer().normalize(title), title, "", "복지", "", support,
                "대중교통 이용 청년", "", "", "기관", title + " " + support, "test", false);
        return projection;
    }
}
