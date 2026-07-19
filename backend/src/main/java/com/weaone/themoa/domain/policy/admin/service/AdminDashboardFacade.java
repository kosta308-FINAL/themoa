package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.admin.dto.dashboard.AdminDashboardResponse;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndexBuilder;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 관리자 운영 화면에 필요한 읽기 전용 상태를 조합한다.
 *
 * <p>정책 수집, Projection 생성, Embedding 처리 같은 기존 작업 로직은 만들거나 복사하지 않고
 * 기존 {@link AdminStatusService}, {@link AdminJobService}, 검색 인덱스 조회 기능만 호출한다.
 * 입력은 없고 출력은 대시보드 표시용 DTO다.</p>
 *
 * <p>본 프로젝트 통합 시 실제 인증/인가가 추가되어도 이 Facade는 관리자 화면 표시 데이터를
 * 조합하는 책임만 유지한다.</p>
 */
@Service
public class AdminDashboardFacade {
    private final AdminStatusService statusService;
    private final AdminJobService jobService;
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder;
    private final PolicySearchProjectionRepository projectionRepository;
    private final SearchReadinessService readinessService;

    public AdminDashboardFacade(AdminStatusService statusService,
                                AdminJobService jobService,
                                PolicyLexicalIndexBuilder lexicalIndexBuilder,
                                PolicySearchProjectionRepository projectionRepository,
                                SearchReadinessService readinessService) {
        this.statusService = statusService;
        this.jobService = jobService;
        this.lexicalIndexBuilder = lexicalIndexBuilder;
        this.projectionRepository = projectionRepository;
        this.readinessService = readinessService;
    }

    public AdminDashboardResponse dashboard() {
        AdminJobStatus currentJob = jobService.latest().orElse(null);
        var readiness = readinessService.readiness();
        var builtAt = lexicalIndexBuilder.cachedBuiltAt();
        var lastProjectionUpdatedAt = projectionRepository.findLastUpdatedAtByProjectionVersion(PolicySearchProjectionService.VERSION);
        Map<String, Object> searchIndex = new LinkedHashMap<>();
        searchIndex.put("ready", readiness.ready());
        searchIndex.put("documentCount", readiness.lexicalIndexDocumentCount());
        searchIndex.put("projectionVersion", PolicySearchProjectionService.VERSION);
        searchIndex.put("projectionCount", readiness.projectionCount());
        searchIndex.put("missingSnapshotCount", projectionRepository.countByMissingSnapshotTrue());
        searchIndex.put("builtAt", builtAt == null ? null : builtAt.toString());
        searchIndex.put("lastProjectionBuiltAt", lastProjectionUpdatedAt == null ? null : lastProjectionUpdatedAt.toString());
        searchIndex.put("missingSteps", readiness.missingSteps());
        searchIndex.put("projectionIndexMismatch", readiness.projectionCount() > 0
                && readiness.lexicalIndexDocumentCount() > 0
                && readiness.projectionCount() != readiness.lexicalIndexDocumentCount());
        return new AdminDashboardResponse(statusService.status(), searchIndex, readiness, currentJob);
    }
}
