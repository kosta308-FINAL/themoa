package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.admin.dto.embedding.AdminEmbeddingItemResponse;
import com.weaone.themoa.domain.policy.admin.dto.embedding.AdminEmbeddingPageResponse;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyEmbeddingSync;
import com.weaone.themoa.domain.policy.policy.repository.PolicyEmbeddingSyncRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 관리자 화면의 Embedding 동기화 목록을 읽기 전용으로 제공한다.
 *
 * <p>Embedding 생성, 재처리, Qdrant 동기화 로직은 기존 작업 서비스가 담당한다. 이 서비스는
 * Entity와 Repository에 저장된 상태, 정책명, 실패 사유만 조회해 화면 DTO로 변환하며 벡터 값이나
 * API Key, 임베딩 원문은 반환하지 않는다.</p>
 *
 * <p>본 프로젝트 통합 시 권한 검증은 Controller 앞단에서 처리하고 이 서비스의 조회 책임은 유지한다.</p>
 */
@Service
public class AdminEmbeddingReadService {
    private final PolicyEmbeddingSyncRepository repository;

    public AdminEmbeddingReadService(PolicyEmbeddingSyncRepository repository) {
        this.repository = repository;
    }

    public AdminEmbeddingPageResponse search(String status, String keyword, int page, int size) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim() : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int resolvedPage = Math.max(0, page);
        int resolvedSize = Math.max(1, Math.min(100, size));
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<PolicyEmbeddingSync> result = repository.searchForAdmin(normalizedStatus, normalizedKeyword, pageable);
        return new AdminEmbeddingPageResponse(
                result.getContent().stream().map(this::toItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    private AdminEmbeddingItemResponse toItem(PolicyEmbeddingSync sync) {
        Policy policy = sync.getPolicy();
        return new AdminEmbeddingItemResponse(
                sync.getId(),
                policy.getId(),
                policy.getSourcePolicyId(),
                policy.getTitle(),
                sync.getSyncStatus(),
                sync.getRequestedAt(),
                sync.getSyncedAt(),
                sync.getRetryCount(),
                "FAILED".equals(sync.getSyncStatus()) ? sync.getLastError() : null
        );
    }
}
