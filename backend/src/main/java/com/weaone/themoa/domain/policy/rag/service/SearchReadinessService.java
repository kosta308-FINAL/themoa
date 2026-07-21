package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.repository.PolicyEmbeddingSyncRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchReadinessService {
    private static final Logger log = LoggerFactory.getLogger(SearchReadinessService.class);

    private final PolicyRepository policyRepository;
    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicyEmbeddingSyncRepository embeddingSyncRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder;
    private final RagProperties ragProperties;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public SearchReadinessService(PolicyRepository policyRepository,
                                  PolicySearchProjectionRepository projectionRepository,
                                  PolicyEmbeddingSyncRepository embeddingSyncRepository,
                                  RegionCodeRepository regionCodeRepository,
                                  PolicyLexicalIndexBuilder lexicalIndexBuilder,
                                  RagProperties ragProperties,
                                  ObjectProvider<VectorStore> vectorStoreProvider) {
        this.policyRepository = policyRepository;
        this.projectionRepository = projectionRepository;
        this.embeddingSyncRepository = embeddingSyncRepository;
        this.regionCodeRepository = regionCodeRepository;
        this.lexicalIndexBuilder = lexicalIndexBuilder;
        this.ragProperties = ragProperties;
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public SearchReadinessResponse readiness() {
        long activePolicyCount = policyRepository.countByActiveTrue();
        long projectionCount = projectionRepository.countByProjectionVersion(PolicySearchProjectionService.VERSION);
        long lexicalIndexDocumentCount = lexicalIndexBuilder.cachedDocumentCount();
        if (activePolicyCount > 0 && projectionCount > 0 && lexicalIndexDocumentCount <= 0) {
            lexicalIndexDocumentCount = recoverLexicalIndex();
        }
        long syncedEmbeddingCount = embeddingSyncRepository.countBySyncStatus("SYNCED");
        long regionCount = regionCodeRepository.count();

        List<String> missingSteps = new ArrayList<>();
        if (regionCount <= 0) {
            missingSteps.add("REGION_CATALOG_SYNC");
        }
        if (activePolicyCount <= 0) {
            missingSteps.add("POLICY_COLLECTION");
        }
        if (activePolicyCount > 0 && projectionCount <= 0) {
            missingSteps.add("SEARCH_PROJECTION_REBUILD");
        }
        if (activePolicyCount > 0 && lexicalIndexDocumentCount <= 0) {
            missingSteps.add("SEARCH_INDEX_REFRESH");
        }
        if (projectionCount > 0 && lexicalIndexDocumentCount > 0 && projectionCount != lexicalIndexDocumentCount) {
            missingSteps.add("SEARCH_INDEX_REFRESH");
        }
        if (!ragProperties.isEnabled()) {
            missingSteps.add("RAG_ENABLED");
        } else {
            if (syncedEmbeddingCount <= 0) {
                missingSteps.add("EMBEDDING_PROCESS");
            }
            if (vectorStoreProvider.getIfAvailable() == null) {
                missingSteps.add("QDRANT_AVAILABLE");
            }
        }

        return new SearchReadinessResponse(missingSteps.isEmpty(), activePolicyCount, projectionCount,
                lexicalIndexDocumentCount, syncedEmbeddingCount, List.copyOf(missingSteps));
    }

    private long recoverLexicalIndex() {
        try {
            return lexicalIndexBuilder.current().size();
        } catch (RuntimeException ex) {
            log.warn("정책 검색 인덱스 자동 복구에 실패했습니다. errorType={}",
                    ex.getClass().getSimpleName());
            return lexicalIndexBuilder.cachedDocumentCount();
        }
    }
}
