package com.weaone.themoa.domain.policy.admin.dto.response;

public record AdminPolicyRawResponse(
        Integer policyId,
        String sourcePolicyId,
        String source,
        String rawPolicy,
        AdminPolicyRawDataResponse pageRawData
) {
}
