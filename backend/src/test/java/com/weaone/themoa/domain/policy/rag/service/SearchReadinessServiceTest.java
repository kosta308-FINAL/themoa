package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.repository.PolicyEmbeddingSyncRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchReadinessServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
    private final PolicyEmbeddingSyncRepository embeddingRepository = mock(PolicyEmbeddingSyncRepository.class);
    private final RegionCodeRepository regionRepository = mock(RegionCodeRepository.class);
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder = mock(PolicyLexicalIndexBuilder.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
    private final RagProperties ragProperties = new RagProperties();

    @Test
    void projectionAndIndexMissingMeansNotReady() {
        ragProperties.setEnabled(true);
        when(regionRepository.count()).thenReturn(20L);
        when(policyRepository.countByActiveTrue()).thenReturn(2650L);
        when(projectionRepository.countByProjectionVersion("policy-search-v2")).thenReturn(0L);
        when(lexicalIndexBuilder.cachedDocumentCount()).thenReturn(0);
        when(embeddingRepository.countBySyncStatus("SYNCED")).thenReturn(2650L);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(mock(VectorStore.class));

        SearchReadinessResponse readiness = service().readiness();

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.missingSteps()).contains("SEARCH_PROJECTION_REBUILD", "SEARCH_INDEX_REFRESH");
    }

    @Test
    void allCountsPresentMeansReady() {
        ragProperties.setEnabled(true);
        when(regionRepository.count()).thenReturn(20L);
        when(policyRepository.countByActiveTrue()).thenReturn(2650L);
        when(projectionRepository.countByProjectionVersion("policy-search-v2")).thenReturn(2650L);
        when(lexicalIndexBuilder.cachedDocumentCount()).thenReturn(2650);
        when(embeddingRepository.countBySyncStatus("SYNCED")).thenReturn(2650L);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(mock(VectorStore.class));

        SearchReadinessResponse readiness = service().readiness();

        assertThat(readiness.ready()).isTrue();
        assertThat(readiness.missingSteps()).isEmpty();
    }

    private SearchReadinessService service() {
        return new SearchReadinessService(policyRepository, projectionRepository, embeddingRepository, regionRepository,
                lexicalIndexBuilder, ragProperties, vectorStoreProvider);
    }
}
