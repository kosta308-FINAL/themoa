package com.weaone.themoa.domain.policy.policy.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.policy.dto.response.PolicyDetailResponse;
import com.weaone.themoa.domain.policy.policy.service.PolicyDetailService;
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

@RestController
@RequestMapping("/api/policies")
public class PolicySearchController {
    private final PolicyRagSearchService searchService;
    private final PolicyDetailService detailService;

    public PolicySearchController(PolicyRagSearchService searchService,
                                  PolicyDetailService detailService) {
        this.searchService = searchService;
        this.detailService = detailService;
    }

    @PostMapping("/search")
    public ApiResponse<PolicySearchResponse> search(@Valid @RequestBody PolicySearchRequest request) {
        return ApiResponse.success(searchService.search(request));
    }

    @GetMapping("/{policyId}")
    public ApiResponse<PolicyDetailResponse> detail(@PathVariable Integer policyId) {
        return ApiResponse.success(detailService.detail(policyId));
    }
}
