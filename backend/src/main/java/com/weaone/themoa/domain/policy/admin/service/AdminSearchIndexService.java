package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchIndexRefreshResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchIndexStatusResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchProjectionRebuildResponse;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndexBuilder;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndex;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService.ProjectionRebuildResult;
import com.weaone.themoa.domain.policy.rag.service.SearchReadinessService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdminSearchIndexService {
    private final PolicySearchProjectionService projectionService;
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder;
    private final PolicySearchProjectionRepository projectionRepository;
    private final SearchReadinessService readinessService;

    public AdminSearchIndexService(PolicySearchProjectionService projectionService,
                                   PolicyLexicalIndexBuilder lexicalIndexBuilder,
                                   PolicySearchProjectionRepository projectionRepository,
                                   SearchReadinessService readinessService) {
        this.projectionService = projectionService;
        this.lexicalIndexBuilder = lexicalIndexBuilder;
        this.projectionRepository = projectionRepository;
        this.readinessService = readinessService;
    }

    public AdminSearchProjectionRebuildResponse rebuildSearchProjection() {
        ProjectionRebuildResult result = projectionService.rebuildAll();
        PolicyLexicalIndex index = lexicalIndexBuilder.refresh();
        return new AdminSearchProjectionRebuildResponse(
                PolicySearchProjectionService.VERSION,
                result.total(),
                result.processed(),
                result.missingSnapshot(),
                index.size()
        );
    }

    public AdminSearchIndexRefreshResponse refreshSearchIndex() {
        PolicyLexicalIndex index = lexicalIndexBuilder.refresh();
        return new AdminSearchIndexRefreshResponse(true, index.size(), index.builtAt().toString());
    }

    public AdminSearchIndexStatusResponse searchIndexStatus() {
        SearchReadinessResponse readiness = readinessService.readiness();
        Instant builtAt = lexicalIndexBuilder.cachedBuiltAt();
        return new AdminSearchIndexStatusResponse(
                readiness.ready(),
                readiness.lexicalIndexDocumentCount(),
                PolicySearchProjectionService.VERSION,
                readiness.projectionCount(),
                projectionRepository.countByMissingSnapshotTrue(),
                builtAt == null ? "" : builtAt.toString(),
                readiness.missingSteps()
        );
    }
}
