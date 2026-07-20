package com.weaone.themoa.domain.policy.sync.service;

import com.weaone.themoa.domain.policy.policy.service.PolicyCollectionResult;
import com.weaone.themoa.domain.policy.policy.service.PolicyRegionRebuildResult;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingProcessResult;
import com.weaone.themoa.domain.policy.rag.service.EmbeddingQueueResult;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService;

public record PolicySyncPipelineResult(
        PolicyCollectionResult collection,
        PolicyRegionRebuildResult regionRebuild,
        PolicySearchProjectionService.ProjectionRebuildResult projectionRebuild,
        int lexicalIndexDocumentCount,
        EmbeddingQueueResult embeddingQueue,
        EmbeddingProcessResult embeddingProcess,
        SearchReadinessResponse readiness
) {
}
