package com.weaone.themoa.domain.policy.policy.controller;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.admin.service.SearchReadinessService;
import com.weaone.themoa.domain.policy.common.exception.SearchDataNotReadyException;
import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyRawData;
import com.weaone.themoa.domain.policy.policy.domain.PolicySourceSnapshot;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResponse;
import com.weaone.themoa.domain.policy.rag.service.PolicyRagSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/policies")
public class PolicySearchController {
    private final PolicyRagSearchService searchService;
    private final PolicyRepository policyRepository;
    private final PolicySourceSnapshotRepository snapshotRepository;
    private final SearchReadinessService readinessService;

    public PolicySearchController(PolicyRagSearchService searchService, PolicyRepository policyRepository,
                                  PolicySourceSnapshotRepository snapshotRepository,
                                  SearchReadinessService readinessService) {
        this.searchService = searchService;
        this.policyRepository = policyRepository;
        this.snapshotRepository = snapshotRepository;
        this.readinessService = readinessService;
    }

    @PostMapping("/search")
    public ApiResponse<PolicySearchResponse> search(@Valid @RequestBody PolicySearchRequest request) {
        var readiness = readinessService.readiness();
        if (!readiness.ready()) {
            throw new SearchDataNotReadyException(readiness);
        }
        return ApiResponse.success(searchService.search(request));
    }

    @GetMapping("/{policyId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Integer policyId) {
        Policy policy = policyRepository.findWithRelationsByIdIn(java.util.List.of(policyId)).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        return ApiResponse.success(Map.of(
                "policyId", policy.getId(),
                "sourcePolicyId", policy.getSourcePolicyId(),
                "title", policy.getTitle(),
                "category", policy.getCategory().name(),
                "agencyName", policy.getAgencyName(),
                "summary", policy.getSummary() == null ? "" : policy.getSummary(),
                "officialUrl", policy.getOfficialUrl() == null ? "" : policy.getOfficialUrl(),
                "status", policy.getStatus(),
                "regions", policy.getRegions().stream().map(region -> region.getRegion().displayName()).toList()
        ));
    }

    @GetMapping("/{policyId}/raw")
    public ApiResponse<Map<String, Object>> raw(@PathVariable Integer policyId) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        PolicySourceSnapshot snapshot = snapshotRepository.findByPolicyId(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        PolicyRawData raw = snapshot.getRawData();
        Map<String, Object> pageRawData = raw == null ? Map.of() : Map.of(
                "rawDataId", raw.getId(),
                "requestUrl", raw.getRequestUrl(),
                "responseFormat", raw.getResponseFormat(),
                "collectedAt", raw.getCollectedAt().toString()
        );
        return ApiResponse.success(Map.of(
                "policyId", policy.getId(),
                "sourcePolicyId", snapshot.getSourcePolicyId(),
                "source", snapshot.getSource(),
                "rawPolicy", snapshot.getRawPolicyJson(),
                "pageRawData", pageRawData
        ));
    }
}
