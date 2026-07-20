package com.weaone.themoa.domain.policy.policy.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.dto.response.PolicyDetailResponse;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PolicyDetailService {
    private final PolicyRepository policyRepository;

    public PolicyDetailService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public PolicyDetailResponse detail(Integer policyId) {
        Policy policy = policyRepository.findWithRelationsByIdIn(List.of(policyId)).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        return new PolicyDetailResponse(
                policy.getId(),
                policy.getSourcePolicyId(),
                policy.getTitle(),
                policy.getCategory().name(),
                policy.getAgencyName(),
                policy.getSummary() == null ? "" : policy.getSummary(),
                policy.getOfficialUrl() == null ? "" : policy.getOfficialUrl(),
                policy.getStatus(),
                policy.getRegions().stream().map(region -> region.getRegion().displayName()).toList()
        );
    }
}
