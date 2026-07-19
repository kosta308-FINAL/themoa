package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminPolicyRawDataResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminPolicyRawResponse;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyRawData;
import com.weaone.themoa.domain.policy.policy.domain.PolicySourceSnapshot;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPolicyRawService {
    private static final AdminPolicyRawDataResponse EMPTY_RAW_DATA =
            new AdminPolicyRawDataResponse(null, null, null, null);

    private final PolicyRepository policyRepository;
    private final PolicySourceSnapshotRepository snapshotRepository;

    public AdminPolicyRawService(PolicyRepository policyRepository,
                                 PolicySourceSnapshotRepository snapshotRepository) {
        this.policyRepository = policyRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public AdminPolicyRawResponse raw(Integer policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        PolicySourceSnapshot snapshot = snapshotRepository.findByPolicyId(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        return new AdminPolicyRawResponse(
                policy.getId(),
                snapshot.getSourcePolicyId(),
                snapshot.getSource(),
                snapshot.getRawPolicyJson(),
                rawData(snapshot.getRawData())
        );
    }

    private AdminPolicyRawDataResponse rawData(PolicyRawData rawData) {
        if (rawData == null) {
            return EMPTY_RAW_DATA;
        }
        return new AdminPolicyRawDataResponse(
                rawData.getId(),
                rawData.getRequestUrl(),
                rawData.getResponseFormat(),
                rawData.getCollectedAt().toString()
        );
    }
}
